package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.common.annotation.DbLock;
import com.huawei.ai_platform.rss.application.service.RssCrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RssCrawlServiceImpl implements RssCrawlService {
    private static final Pattern JDBC_PATTERN = Pattern.compile(
            "jdbc:postgresql://([^:]+):(\\d+)/(.+)");

    @Value("${ai.crawl.script-path:ai-crawl/vibe-main.py}")
    private String crawlScriptPath;

    @Value("${ai.crawl.python-path:python}")
    private String pythonPath;

    @Value("${ai.crawl.pip-path:pip}")
    private String pipPath;

    @Value("${ai.crawl.requirements-path:ai-crawl/requirements.txt}")
    private String requirementsPath;

    @Value("${ai.crawl.llm-url:https://openrouter.ai/api/v1}")
    private String llmUrl;

    @Value("${ai.crawl.llm-key:}")
    private String llmKey;

    @Value("${ai.crawl.llm-model:deepseek-v4-flash}")
    private String llmModel;

    @Value("${ai.crawl.llm-provider:deepseek}")
    private String llmProvider;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Override
    @DbLock(category = "ai_crawl_lock")
    public OperationResult runCrawl() {
        log.info("Starting AI crawl");

        try {
            ensureDependencies();

            Path scriptDir = Files.createTempDirectory("ai-crawl");
            Path scriptFile = scriptDir.resolve("vibe-main.py");

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(crawlScriptPath)) {
                if (in == null) {
                    throw new IllegalStateException("Crawl script not found on classpath: " + crawlScriptPath);
                }
                Files.copy(in, scriptFile, StandardCopyOption.REPLACE_EXISTING);
            }

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptFile.getFileName().toString());
            pb.redirectErrorStream(true);
            pb.directory(scriptDir.toFile());
            pb.environment().put("LLM_URL", llmUrl);
            pb.environment().put("LLM_KEY", llmKey);
            pb.environment().put("LLM_MODEL", llmModel);
            pb.environment().put("LLM_PROVIDER", llmProvider);

            Matcher m = JDBC_PATTERN.matcher(datasourceUrl);
            if (m.find()) {
                pb.environment().put("DB_HOST", m.group(1));
                pb.environment().put("DB_PORT", m.group(2));
                pb.environment().put("DB_NAME", m.group(3));
            }
            pb.environment().put("DB_USER", datasourceUsername);
            pb.environment().put("DB_PASSWORD", datasourcePassword);
            pb.environment().put("PYTHONHTTPSVERIFY", "0");
            pb.environment().put("REQUESTS_CA_BUNDLE", "");
            pb.environment().put("CURL_CA_BUNDLE", "");
            pb.environment().put("SSL_CERT_FILE", "");

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[crawl] {}", line);
                }
            }

            int exitCode = process.waitFor();

            try {
                Files.deleteIfExists(scriptFile);
                Files.deleteIfExists(scriptDir);
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup temp crawl directory: {}", cleanupEx.getMessage());
            }

            if (exitCode == 0) {
                log.info("AI crawl completed successfully");
                return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                        .reason("AI crawl completed successfully").build();
            } else {
                log.error("AI crawl failed with exit code {}", exitCode);
                return OperationResult.builder().state(OperationResultEnum.FAILURE)
                        .reason("AI crawl failed with exit code " + exitCode).build();
            }
        } catch (Exception e) {
            log.error("AI crawl error", e);
            return OperationResult.builder().state(OperationResultEnum.FAILURE)
                    .reason("AI crawl error: " + e.getMessage()).build();
        }
    }

    @Override
    public OperationResult runCrawlAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                OperationResult result = runCrawl();
                if (result.isFailed()) {
                    log.error("AI crawl failed: {}", result.getInfo());
                }
            } catch (Exception e) {
                log.error("AI crawl error", e);
            }
        });
        return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                .reason("AI crawl started").build();
    }

    private void ensureDependencies() throws Exception {
        Path reqFile = Files.createTempFile("requirements", ".txt");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(requirementsPath)) {
            if (in == null) {
                log.warn("requirements.txt not found on classpath: {}, skipping pip install", requirementsPath);
                return;
            }
            Files.copy(in, reqFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Installing/updating Python dependencies");
        ProcessBuilder pb = new ProcessBuilder(pipPath, "install", "--upgrade",
                "--trusted-host", "pypi.org",
                "--trusted-host", "files.pythonhosted.org",
                "-r", reqFile.toString());
        pb.redirectErrorStream(true);
        pb.environment().put("PYTHONHTTPSVERIFY", "0");
        pb.environment().put("REQUESTS_CA_BUNDLE", "");
        pb.environment().put("CURL_CA_BUNDLE", "");
        pb.environment().put("SSL_CERT_FILE", "");
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[pip] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("pip install exited with code {}", exitCode);
        }

        Files.deleteIfExists(reqFile);
    }
}

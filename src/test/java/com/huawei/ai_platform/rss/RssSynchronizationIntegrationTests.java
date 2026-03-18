package com.huawei.ai_platform.rss;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.huawei.ai_platform.common.Constant.ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class RssSynchronizationIntegrationTests {
    private static final String USERNAME = "artem";
    private static final String PASSWORD = "12345";
    private static final String DB_NAME = "freshrss";

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RssSyncService rssSyncService;

    @Autowired
    private RssDao rssDao;

    @Value("${cloud.directories.articles}")
    private String articles;

    @Container
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine")
            .withUsername(USERNAME).withPassword(PASSWORD).withDatabaseName(DB_NAME).withReuse(true);

    @DynamicPropertySource
    private static void customProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);

        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.changelog", () -> "db/changelog/changelog-master.xml");
    }

    @Test
    public void test_uploadNews_ShouldBeAllUploadedFromDataSource() {
        clearCloud();

        // Business functionality
        LocalDateTime date = LocalDateTime.now().minusDays(1L);
        rssSyncService.uploadNewArticles(date);

        // Check side
        List<RssArticleCloud> articleClouds = getArticlesFromCloud();

        long startDate = getAsMicro(date.with(LocalTime.MIN));
        long endDate = getAsMicro(date.with(LocalTime.MAX));
        List<RssFetchData> articlesFromDb = rssDao.queryArticlesBy(startDate, endDate);

        // 1. Check that all reports uploaded
        Assertions.assertThat(articleClouds.size()).withFailMessage("The count items in the cloud and DB is not the same")
                .isEqualTo(articlesFromDb.size());

        // 2. Check that all ID's in cloud and DB is the same
        Set<Long> setFromCloud = articleClouds.stream().map(RssArticleCloud::getArticleId).collect(Collectors.toSet());
        Set<Long> setFromDb = articlesFromDb.stream().map(RssFetchData::getId).collect(Collectors.toSet());

        setFromCloud.removeAll(setFromDb);
        Assertions.assertThat(setFromCloud).withFailMessage("Not equal ID's: '%s'",
                setFromCloud.stream().map(Object::toString).collect(Collectors.joining(","))).isEmpty();

        clearCloud();
    }

    /**
     * Reads cloud info
     *
     * @return List of an articles
     */
    private List<RssArticleCloud> getArticlesFromCloud() {
        String pathString = environment.getProperty("BASE_PATH_CLOUD");
        assertNotNull(pathString, "Environment path is null");

        Path path = Path.of(pathString, articles);
        try (Stream<Path> streamPath = Files.walk(path)) {
            List<RssArticleCloud> resultList = new ArrayList<>();

            List<Path> pathList = streamPath.filter(Files::isRegularFile).sorted(Comparator.reverseOrder()).toList();
            for (Path item : pathList) {
                String byteData = Files.readString(item);
                List<RssArticleCloud> listDataForFile = objectMapper.readValue(byteData, new TypeReference<>() {
                });

                resultList.addAll(listDataForFile);
            }

            return resultList;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Extracts date as micro value
     *
     * @param forDate for which date do you want extract data
     * @return microseconds
     */
    private long getAsMicro(LocalDateTime forDate) {
        Instant instant = forDate.atZone(ZONE).toInstant();
        long epochSecond = instant.getEpochSecond();
        int nanoAdjustment = instant.getNano();

        return epochSecond * 1_000_000L + nanoAdjustment / 1_000L;
    }

    /**
     * Another method that clears environment before or after his work
     */
    private void clearCloud() {
        String path = environment.getProperty("BASE_PATH_CLOUD");
        assertNotNull(path, "Path cannot be null");

        Path pathToRemove = Path.of(path);

        if (!Files.exists(pathToRemove)) {
            try {
                Files.createDirectories(pathToRemove);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        try (Stream<Path> streamPath = Files.walk(pathToRemove)) {
            List<Path> sortedPath = streamPath.sorted(Comparator.reverseOrder()).toList();
            for (Path item : sortedPath) {
                if (!item.equals(pathToRemove)) {
                    Files.delete(item);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("An error has occurred during preparing env: " + exception.getMessage());
        }

    }
}

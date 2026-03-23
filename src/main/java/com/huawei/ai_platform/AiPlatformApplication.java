package com.huawei.ai_platform;

import com.huawei.ai_platform.rss.infrastructure.job.RssJob;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main application class
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@SpringBootApplication
@MapperScan({
        "com.huawei.ai_platform.rss.infrastructure.persistence"
})
public class AiPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiPlatformApplication.class, args);
	}

    @Bean
    public CommandLineRunner runner(RssJob rssJob) {
        return args -> rssJob.runTranslation();
    }
}

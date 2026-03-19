package com.huawei.ai_platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}

package com.huawei.ai_platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Main application class
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@SpringBootApplication
@MapperScan({
        "com.huawei.ai_platform.rss.infrastructure.persistence",
        "com.huawei.ai_platform.lock.infrastructure.persistence"
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AiPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiPlatformApplication.class, args);
	}
}

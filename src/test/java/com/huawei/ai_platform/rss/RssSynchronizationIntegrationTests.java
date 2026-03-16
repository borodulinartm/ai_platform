package com.huawei.ai_platform.rss;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class RssSynchronizationIntegrationTests {
    private static final String USERNAME = "artem";
    private static final String PASSWORD = "12345";
    private static final String DB_NAME = "freshrss";

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
    public void testFoo() {
        int i =1 ;
    }
}

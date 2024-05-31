package it.gov.acn;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Configuration
@Testcontainers
public class PostgresTestContainerConfiguration {

    @Container
    public static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    static {
        postgresContainer.start();
        System.out.println("PostgreSQL container started with URL: " + postgresContainer.getJdbcUrl());
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        String jdbcUrlPart = postgresContainer.getJdbcUrl();
        String jdbcUrlFull = jdbcUrlPart + "&TC_DAEMON=true";

        registry.add("spring.datasource.url", () -> jdbcUrlFull);
        registry.add("spring.datasource.driver-class-name", org.postgresql.Driver.class::getName);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}

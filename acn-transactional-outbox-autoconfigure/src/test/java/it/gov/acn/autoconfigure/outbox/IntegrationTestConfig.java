package it.gov.acn.autoconfigure.outbox;

import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
@ActiveProfiles("src/main/test")
public class IntegrationTestConfig {

  protected final static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.11").withReuse(true);

  static {
    postgres.start();
    if (Boolean.valueOf(System.getenv().getOrDefault("TESTCONTAINERS_RYUK_DISABLED", "false"))) {
      Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
    }
  }


  @Bean
  public AuditorAware<String> auditorAwareLocal() {
    return () -> Optional.of("JUnit");
  }

  @DynamicPropertySource
  static void pgProperties(DynamicPropertyRegistry registry) {
    String jdbcUrlPart = postgres.getJdbcUrl();
    String jdbcUrlFull = jdbcUrlPart + "&TC_DAEMON=true";

    registry.add("spring.datasource.url", () -> jdbcUrlFull);
    registry.add("spring.datasource.driver-class-name", org.postgresql.Driver.class::getName);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Bean
  public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource);
    initializer.setDatabasePopulator(databasePopulator());
    return initializer;
  }

  private DatabasePopulator databasePopulator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("postgres_schema.sql"));
    populator.addScript(new ClassPathResource("postgres_data.sql"));
    return populator;
  }
}
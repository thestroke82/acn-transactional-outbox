package it.gov.acn.autoconfigure.outbox;

import it.gov.acn.autoconfigure.outbox.config.BulkheadAutoConfiguration;
import it.gov.acn.autoconfigure.outbox.config.OutboxAutoconfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Optional;

@TestConfiguration
@ActiveProfiles("test")
public  class IntegrationTestConfig {
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

		registry.add("spring.datasource.url", ()->jdbcUrlFull);
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
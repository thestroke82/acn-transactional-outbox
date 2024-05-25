package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.condition.ContextValidCondition;
import it.gov.acn.autoconfigure.outbox.condition.StarterEnabled;
import it.gov.acn.autoconfigure.outbox.manager.OutboxManager;
import it.gov.acn.autoconfigure.outbox.manager.OutboxManagerImpl;
import it.gov.acn.autoconfigure.outbox.providers.postgres.PostgresJdbcDataProvider;
import it.gov.acn.outbox.core.OutboxConfiguration;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.SchedulingProvider;
import it.gov.acn.outbox.scheduler.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@AutoConfiguration(after= {
    BulkheadAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    BatchAutoConfiguration.class
})
public class OutboxAutoconfiguration {
    private final Logger logger = LoggerFactory.getLogger(OutboxAutoconfiguration.class);

    // a task scheduler is needed to schedule the outbox processor
    // it will be created only if there is no other task scheduler in the context
    // and the outbox starter is enabled, and the context is valid
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(
            "ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }



    // a data provider is needed to fetch the outbox messages
    // it will be created only if the context is valid and the outbox is enabled
    @Bean
    @ConditionalOnBean(DataSource.class)
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public DataProvider dataProvider(DataSource dataSource){
        // TODO: Factory method to create a DataProvider
        return new PostgresJdbcDataProvider(dataSource);
    }

    // the outbox manager is the interface to the client code to record events
    @Bean
    @ConditionalOnBean(DataProvider.class)
    public OutboxManager outboxManager(DataProvider dataProvider){
        return new OutboxManagerImpl(dataProvider);
    }

    // That's the main bean that will be created only if the conditions at the end of
    // the conditions chain
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public OutboxScheduler transactionalOutboxScheduler(
        OutboxProperties transactionalOutboxProperties,
        TaskScheduler taskScheduler,
        DataProvider dataProvider
    ){
        logger.debug("Transactional Outbox Starter configuration details: {}",transactionalOutboxProperties);
        OutboxConfiguration outboxConfiguration = OutboxConfiguration.builder()
            .fixedDelay(transactionalOutboxProperties.getFixedDelay())
            .maxAttempts(transactionalOutboxProperties.getMaxAttempts())
            .backoffBase(transactionalOutboxProperties.getBackoffBase())
            .dataProvider(dataProvider)
            .schedulingProvider((task, delay) ->
                    taskScheduler.scheduleWithFixedDelay(task, Duration.of(delay, ChronoUnit.MILLIS)))
            .build();
        return new OutboxScheduler(outboxConfiguration);
    }
}

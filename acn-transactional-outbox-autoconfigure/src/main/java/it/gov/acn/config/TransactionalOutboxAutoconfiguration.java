package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.ContextValidCondition;
import it.gov.acn.condition.StarterEnabled;
import it.gov.acn.outboxprocessor.model.DataProvider;
import it.gov.acn.providers.JdbcDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;

@AutoConfiguration(after= {
    BulkheadAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    BatchAutoConfiguration.class
})
public class TransactionalOutboxAutoconfiguration {
    private final Logger logger = LoggerFactory.getLogger(TransactionalOutboxAutoconfiguration.class);

    @Bean
    public Object bulkhead1(
        TransactionalOutboxProperties transactionalOutboxProperties
    ) {
        return new Object();
    }

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
        return new JdbcDataProvider();
    }

    // That's the main bean that will be created only if the conditions at the end of
    // the conditions chain
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public TransactionalOutboxScheduler transactionalOutboxScheduler(
        TransactionalOutboxProperties transactionalOutboxProperties,
        TaskScheduler taskScheduler,
        DataProvider dataProvider
    ){
        logger.debug("Transactional Outbox Starter configuration details: {}",transactionalOutboxProperties);
        return new TransactionalOutboxScheduler(transactionalOutboxProperties, taskScheduler, dataProvider);
    }
}

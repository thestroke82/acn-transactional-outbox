package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.OutboxEnabledCondition;
import it.gov.acn.condition.PropertiesValidCondition;
import it.gov.acn.config.ErrorMessagesHolder.ErrorReporter;
import it.gov.acn.context.InvalidContextBulkhead;
import it.gov.acn.context.ValidContextBulkhead;
import it.gov.acn.context.ValidPropertiesBulkhead;
import it.gov.acn.outboxprocessor.model.DataProvider;
import it.gov.acn.providers.JdbcDataProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(after= {
    JpaRepositoriesAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    BatchAutoConfiguration.class
})
@EnableConfigurationProperties(TransactionalOutboxProperties.class)
public class TransactionalOutboxAutoconfiguration {
    private final Logger logger = LoggerFactory.getLogger(TransactionalOutboxAutoconfiguration.class);

    private final TransactionalOutboxProperties transactionalOutboxProperties;

    public TransactionalOutboxAutoconfiguration(TransactionalOutboxProperties transactionalOutboxProperties){
        this.transactionalOutboxProperties = transactionalOutboxProperties;
    }

    // To activate the outbox starter, two conditions must be met:
    // 1. the properties must be valid and the enabled flag must be true
    // 2. a datasource and a transaction manager must be present in the context
    // For lack of a better mechanism, a combination of conditional beans will be used

    // this is a conditional bean that will be created only if the properties are valid
    // and the outbox starter is enabled
    @Bean
    @Conditional({
            PropertiesValidCondition.class,
            OutboxEnabledCondition.class
    })
    public ValidPropertiesBulkhead validPropertiesBulkhead(){
        return new ValidPropertiesBulkhead();
    }

    // this is a conditional bean that will be created only if the context is adequate
    @Bean
    @ConditionalOnBean({
        ValidPropertiesBulkhead.class,
        DataSource.class,
        PlatformTransactionManager.class
    })
    public ValidContextBulkhead validContextBulkhead(){
        return new ValidContextBulkhead();
    }

    // It's ugly, I know, but it's the only way to report errors about missing beans

    // if the DataSource is missing, an error will be reported
    @Bean
    @ConditionalOnBean(ValidPropertiesBulkhead.class) // should log context problems only if the properties are valid
    @ConditionalOnMissingBean(DataSource.class)
    public ErrorReporter errorReporterDatasource(){
        return new ErrorReporter("DataSource is missing in context");
    }

    // if the PlatformTransactionManager is missing, an error will be reported
    @Bean
    @ConditionalOnMissingBean( PlatformTransactionManager.class)
    @ConditionalOnBean(ValidPropertiesBulkhead.class) // should log context problems only if the properties are valid
    public ErrorReporter errorReporterTransactionManager(){
        return new ErrorReporter("TransactionManager is missing in context");
    }

    // At this juncture, there are two possible scenarios:
    // If the properties are valid, the enabled flag is true, and the context is appropriate, the behavior will commence.
    // If there is an issue or something is missing, the errors will be reported and the behavior will not start.


    // a task scheduler is needed to schedule the outbox processor
    // it will be created only if there is no other task scheduler in the context
    @Bean
    @ConditionalOnBean(ValidContextBulkhead.class)
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
    // it will be created only if there is a datasource and a transaction manager in context
    // and of course if the properties are valid
    // If a ValidContextBulkhead is present, a DataSource and a PlatformTransactionManager are present
    @Bean
    @ConditionalOnBean(ValidContextBulkhead.class)
    public DataProvider dataProvider(DataSource dataSource){
        // TODO: Factory method to create a DataProvider
        return new JdbcDataProvider();
    }



    // That's the main bean that will be created only if the conditions are met
    // Note that ValidContextBulkhead it's superfluous here, but I'll keep it for clarity
    @Bean
    @ConditionalOnBean({
            ValidContextBulkhead.class,
            TaskScheduler.class,
            DataProvider.class
    })
    public TransactionalOutboxScheduler transactionalOutboxScheduler(
            TransactionalOutboxProperties transactionalOutboxProperties,
            TaskScheduler taskScheduler,
            DataProvider dataProvider
    ){
        logger.debug("Transactional Outbox Starter configuration details: {}",transactionalOutboxProperties);
        return new TransactionalOutboxScheduler(transactionalOutboxProperties, taskScheduler, dataProvider);
    }

    // this is used to report errors if the conditions are not met
    @Bean
    @ConditionalOnMissingBean(ValidContextBulkhead.class)
    public InvalidContextBulkhead invalidContextBulkhead(){
        if(!ErrorMessagesHolder.getErrorMessages().isEmpty()){
            logger.error("Transactional Outbox Starter configuration is not valid: "+
                String.join(", ",ErrorMessagesHolder.getErrorMessages()));
        }
        return new InvalidContextBulkhead();
    }
}

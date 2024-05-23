package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.OutboxEnabledCondition;
import it.gov.acn.condition.PropertiesValidCondition;
import it.gov.acn.config.ErrorMessagesHolder.ErrorReporter;
import it.gov.acn.context.InvalidContextBulkhead;
import it.gov.acn.context.ValidContextBulkhead;
import it.gov.acn.context.ValidPropertiesBulkhead;
import it.gov.acn.etc.BeanGarbageCollector;
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
import org.springframework.context.ApplicationContext;
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
        return BeanGarbageCollector.registerTemporaryBean(new ValidPropertiesBulkhead());
    }

    // this is a conditional bean that will be created only if the context is adequate
    @Bean
    @ConditionalOnBean({
        ValidPropertiesBulkhead.class,
        DataSource.class,
        PlatformTransactionManager.class
    })
    public ValidContextBulkhead validContextBulkhead(){
        return BeanGarbageCollector.registerTemporaryBean(new ValidContextBulkhead());
    }

    // It's ugly, I know, but it's the only way to report errors about missing beans

    // if the DataSource is missing, an error will be reported
    @Bean
    @ConditionalOnBean(ValidPropertiesBulkhead.class) // should log context problems only if the properties are valid
    @ConditionalOnMissingBean(DataSource.class)
    public ErrorReporter errorReporterDatasource(){
        return BeanGarbageCollector.registerTemporaryBean(
            new ErrorReporter("DataSource is missing in context"));
    }

    // if the PlatformTransactionManager is missing, an error will be reported
    @Bean
    @ConditionalOnMissingBean( PlatformTransactionManager.class)
    @ConditionalOnBean(ValidPropertiesBulkhead.class) // should log context problems only if the properties are valid
    public ErrorReporter errorReporterTransactionManager(){
        return BeanGarbageCollector.registerTemporaryBean(
            new ErrorReporter("TransactionManager is missing in context"));
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
        return BeanGarbageCollector.registerCoreBean(threadPoolTaskScheduler);
    }


    // a data provider is needed to fetch the outbox messages
    // it will be created only if there is a datasource and a transaction manager in context
    // and of course if the properties are valid
    // If a ValidContextBulkhead is present, a DataSource and a PlatformTransactionManager are present
    @Bean
    @ConditionalOnBean(ValidContextBulkhead.class)
    public DataProvider dataProvider(DataSource dataSource){
        // TODO: Factory method to create a DataProvider
        return BeanGarbageCollector.registerCoreBean(new JdbcDataProvider());
    }


    // At this point, the last two beans are created only if the conditions are met or not
    // In the first case, the outbox processor will be scheduled and the behaviour started
    // In the second case, the errors will be reported and the behaviour will not start
    // Either are terminal points of the Conditional chain implemented in this configuration

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
            DataProvider dataProvider,
            ApplicationContext applicationContext
    ){
        logger.debug("Transactional Outbox Starter configuration details: {}",transactionalOutboxProperties);
        BeanGarbageCollector.destroyTemporaryBeans(applicationContext);
        return new TransactionalOutboxScheduler(transactionalOutboxProperties, taskScheduler, dataProvider);
    }

    // this is used to report errors if the conditions are not met
    @Bean
    @ConditionalOnMissingBean(ValidContextBulkhead.class)
    public InvalidContextBulkhead invalidContextBulkhead(ApplicationContext applicationContext){
        if(!ErrorMessagesHolder.getErrorMessages().isEmpty()){
            logger.error("Transactional Outbox Starter configuration is not valid: "+
                String.join(", ",ErrorMessagesHolder.getErrorMessages()));
        }
        BeanGarbageCollector.destroyTemporaryBeans(applicationContext);
        BeanGarbageCollector.destroyCoreBeans(applicationContext);
        return null;
    }
}

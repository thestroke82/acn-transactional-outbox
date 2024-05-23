package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.OutboxEnabledCondition;
import it.gov.acn.condition.PropertiesValidCondition;
import it.gov.acn.config.ErrorMessagesHolder.ErrorReporter;
import it.gov.acn.context.ValidPropertiesBulkhead;
import it.gov.acn.outboxprocessor.model.DataProvider;
import it.gov.acn.providers.JdbcDataProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@EnableConfigurationProperties(TransactionalOutboxProperties.class)
public class TransactionalOutboxAutoconfiguration {
    private final Logger logger = LoggerFactory.getLogger(TransactionalOutboxAutoconfiguration.class);

    private final TransactionalOutboxProperties transactionalOutboxProperties;

    public TransactionalOutboxAutoconfiguration(TransactionalOutboxProperties transactionalOutboxProperties){
        this.transactionalOutboxProperties = transactionalOutboxProperties;
    }

    // this is a conditional bean that will be created only if the properties are valid
    // if the properties are not valid, the starter will not bootstrap (see below   )
    @Bean
    @Conditional({
            PropertiesValidCondition.class,
            OutboxEnabledCondition.class
    })
    public ValidPropertiesBulkhead validPropertiesBulkhead(){
        return new ValidPropertiesBulkhead();
    }



    // a task scheduler is needed to schedule the outbox processor
    // it will be created only if there is no other task scheduler in the context
    // and of course if the properties are valid
    @Bean
    @ConditionalOnBean(ValidPropertiesBulkhead.class)
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
    @Bean
    @ConditionalOnBean({
        ValidPropertiesBulkhead.class,
        DataSource.class,
        PlatformTransactionManager.class
    })
    public DataProvider dataProvider(DataSource dataSource){
        // TODO: Factory method to create a DataProvider
        return new JdbcDataProvider();
    }

    // if the DataSource is missing, an error will be reported
    @Bean
    @ConditionalOnMissingBean({
        DataSource.class
    })
    public ErrorReporter errorReporterDatasource(){
        return new ErrorReporter("DataSource is missing in context");
    }

    // if the PlatformTransactionManager is missing, an error will be reported
    @Bean
    @ConditionalOnMissingBean({
        PlatformTransactionManager.class
    })
    public ErrorReporter errorReporterTransactionManager(){
        return new ErrorReporter("PlatformTransactionManager is missing in context");
    }

    @Bean
    @ConditionalOnBean({
            ValidPropertiesBulkhead.class,
            DataProvider.class,
            DataSource.class
    })
    public TransactionalOutboxScheduler transactionalOutboxScheduler(
            TransactionalOutboxProperties transactionalOutboxProperties,
            TaskScheduler taskScheduler,
            DataProvider dataProvider
    ){
        return new TransactionalOutboxScheduler(transactionalOutboxProperties, taskScheduler, dataProvider);
    }


    @PostConstruct
    public void logContextStatus(){
        if(ErrorMessagesHolder.getErrorMessages().isEmpty()){
            logger.debug("Transactional Outbox Starter configuration details: {}",transactionalOutboxProperties);
        }else{
            logger.error("Transactional Outbox Starter configuration is not valid: "+
                     String.join(", ",ErrorMessagesHolder.getErrorMessages()));
        }
    }
}

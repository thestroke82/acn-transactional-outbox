package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.OutboxEnabledCondition;
import it.gov.acn.condition.PropertiesValidCondition;
import it.gov.acn.context.ValidContextBulkhead;
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

@AutoConfiguration
@EnableConfigurationProperties(TransactionalOutboxProperties.class)
public class TransactionalOutboxAutoconfiguration {
    private Logger logger = LoggerFactory.getLogger(TransactionalOutboxAutoconfiguration.class);

    private TransactionalOutboxProperties transactionalOutboxProperties;

    public TransactionalOutboxAutoconfiguration(
            TransactionalOutboxProperties transactionalOutboxProperties
    ){
        this.transactionalOutboxProperties = transactionalOutboxProperties;
    }

    @Bean
    @Conditional({
            PropertiesValidCondition.class,
            OutboxEnabledCondition.class
    })
    @ConditionalOnBean(DataSource.class)
    public ValidContextBulkhead validContextBulkhead(DataSource dataSource){
        return new ValidContextBulkhead();
    }

    @Bean(name = "dataSourceNotFoundErrorMessageReport")
    @ConditionalOnMissingBean(DataSource.class)
    public ErrorMessagesHolder.ErrorMessageReport dataSourceNotFoundErrorMessageReport(){
        return new ErrorMessagesHolder.ErrorMessageReport("No DataSource found in context");
    }


    @Bean
    @ConditionalOnBean(ValidContextBulkhead.class)
    @ConditionalOnMissingBean(TaskScheduler.class) // Create a TaskScheduler bean only if there is none in the context
    public TaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(
                "ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }


    @Bean
    @ConditionalOnBean({
            ValidContextBulkhead.class,
            TaskScheduler.class
    })
    public TransactionalOutboxScheduler transactionalOutboxScheduler(
            TransactionalOutboxProperties transactionalOutboxProperties,
            TaskScheduler taskScheduler
    ){
        return new TransactionalOutboxScheduler(transactionalOutboxProperties, taskScheduler);
    }


    @PostConstruct
    public void logContextStatus(){
        if(ErrorMessagesHolder.getErrorMessages().isEmpty()){
            logger.info("Transactional Outbox Configuration Details: {}",transactionalOutboxProperties);
        }else{
            logger.error("Your application context is not valid: "+
                     String.join(", ",ErrorMessagesHolder.getErrorMessages()));
        }
    }
}

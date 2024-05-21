package it.gov.acn.config;

import it.gov.acn.TransactionalOutboxScheduler;
import it.gov.acn.condition.ContextValidCondition;
import it.gov.acn.condition.OutboxEnabledCondition;
import it.gov.acn.condition.PropertiesValidCondition;
import it.gov.acn.context.ContextBulkhead;
import it.gov.acn.context.ContextUtils;
import it.gov.acn.context.InvalidContextBulkhead;
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
import org.springframework.context.annotation.DependsOn;
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
    public ContextBulkhead contextBulkhead(DataSource dataSource){
        // the starter requires a Postgres DataSource bean to be present in context
        if(ContextUtils.isPostgresDatasource(dataSource)){
            return new ValidContextBulkhead();
        }else{
            ErrorMessagesHolder.addErrorMessage("No Postgres DataSource found in context");
            return new InvalidContextBulkhead();
        }
    }

    @Bean(name = "dataSourceNotFoundErrorMessageReport")
    @ConditionalOnMissingBean(DataSource.class)
    public ErrorMessagesHolder.ErrorMessageReport dataSourceNotFoundErrorMessageReport(){
        return new ErrorMessagesHolder.ErrorMessageReport("No DataSource found in context");
    }


    @Bean
    @DependsOn("contextBulkhead")
    @Conditional(ContextValidCondition.class)
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
    @ConditionalOnBean(TaskScheduler.class)
    @DependsOn("contextBulkhead")
    @Conditional(ContextValidCondition.class)
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

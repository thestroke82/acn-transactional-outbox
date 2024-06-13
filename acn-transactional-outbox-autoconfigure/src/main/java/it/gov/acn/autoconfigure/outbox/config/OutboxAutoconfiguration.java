package it.gov.acn.autoconfigure.outbox.config;

import io.micrometer.core.instrument.MeterRegistry;
import it.gov.acn.autoconfigure.outbox.condition.ContextValidCondition;
import it.gov.acn.autoconfigure.outbox.condition.StarterEnabled;
import it.gov.acn.autoconfigure.outbox.observability.OutboxPrometheusExposer;
import it.gov.acn.autoconfigure.outbox.providers.data.postgres.PostgresJdbcDataProvider;
import it.gov.acn.autoconfigure.outbox.providers.locking.SchedlockLockProvider;
import it.gov.acn.autoconfigure.outbox.providers.scheduling.TaskSchedulerSchedulingProvider;
import it.gov.acn.autoconfigure.outbox.providers.serialization.JacksonSerializationProvider;
import it.gov.acn.autoconfigure.outbox.providers.transaction.TransactionTemplateProvider;
import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;
import it.gov.acn.outbox.core.processor.OutboxProcessorFactory;
import it.gov.acn.outbox.core.recorder.DatabaseOutboxEventRecorder;
import it.gov.acn.outbox.core.recorder.DummyOutboxEventRecorder;
import it.gov.acn.outbox.core.recorder.OutboxEventRecorder;
import it.gov.acn.outbox.core.scheduler.OutboxScheduler;
import it.gov.acn.outbox.core.scheduler.OutboxSchedulerFactory;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.LockingProvider;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;
import it.gov.acn.outbox.provider.SchedulingProvider;
import it.gov.acn.outbox.provider.SerializationProvider;
import it.gov.acn.outbox.provider.TransactionManagerProvider;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
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
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(after= {
    BulkheadAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    BatchAutoConfiguration.class,
    PrometheusMetricsExportAutoConfiguration.class
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



    // a data provider is needed by the core to fetch and update the outbox items
    @Bean
    @ConditionalOnBean(DataSource.class)
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public DataProvider dataProvider(DataSource dataSource){
        // TODO: Factory method to create a DataProvider, for now there is only one implementation
        return new PostgresJdbcDataProvider(dataSource);
    }

    // a transaction manager provider is needed by the core to handle transactions
    @Bean
    @ConditionalOnBean(PlatformTransactionManager.class)
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public TransactionTemplateProvider transactionManagerProvider(){
        // TODO: Factory method to create a TransactionTemplateProvider, for now there is only one implementation
        return new TransactionTemplateProvider();
    }

    // a scheduling provider is needed by the core to schedule the outbox processor
    @Bean
    @ConditionalOnBean(TaskScheduler.class)
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public SchedulingProvider schedulingProvider(TaskScheduler taskScheduler){
        // TODO: Factory method to create a SchedulingProvider, for now there is only one implementation
        return new TaskSchedulerSchedulingProvider(taskScheduler);
    }

    // a serialization provider is needed by the core to serialize the events
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public SerializationProvider serializationProvider(){
        // TODO: Factory method to create a SerializationProvider, for now there is only one implementation
        return new JacksonSerializationProvider();
    }


    // if the application is already using shedlock there is a chance that has
    // already a lock provider, so we can reuse it
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider lockProvider(DataSource dataSource){
        return new JdbcTemplateLockProvider(dataSource);
    }

    // a locking provider is needed by the core to lock the outbox processor
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    public LockingProvider lockingProvider(LockProvider lockProvider){
        // TODO: Factory method to create a LockingProvider, for now there is only one implementation
        return new SchedlockLockProvider(lockProvider);
    }

    // a last provider is needed: outbox item provider to handle the outbox items
    // note: this provider must be implemented by the !client code! at this point we have
    // already checked that an implementation is present in the context

    // the outbox recorder serves as a bridge for the client code to register events
    @Bean
    @ConditionalOnBean({
        DataProvider.class,
        SerializationProvider.class,
        TransactionManagerProvider.class
    })
    public OutboxEventRecorder outboxEventRecorder(
        DataProvider dataProvider,
        SerializationProvider serializationProvider,
        TransactionManagerProvider transactionManagerProvider
    ){
        return new DatabaseOutboxEventRecorder(dataProvider, serializationProvider, transactionManagerProvider);
    }

    // We set up a dummy outbox event recorder so the client code can still work even if the outbox is not enabled
    // note that is a conditional bean, it will only be created if the real outbox event recorder is not present
    @Bean
    @Conditional({
        ContextValidCondition.class
    })
    @ConditionalOnMissingBean(name = "outboxEventRecorder")
    public OutboxEventRecorder dummyOutboxEventRecorder(){
        return new DummyOutboxEventRecorder();
    }


    // That's the bean responsible for exposing the outbox metrics to prometheus
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnBean({
        MeterRegistry.class
    })
    public OutboxPrometheusExposer outboxPrometheusExposer(MeterRegistry meterRegistry){
        return new OutboxPrometheusExposer(meterRegistry);
    }

    // That's the bean that represents the outbox core configuration, i.e. the configuration
    // needed by the core to implement the outbox behavior
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnBean({
        DataProvider.class,
        SchedulingProvider.class,
        SerializationProvider.class,
        LockingProvider.class,
    })
    // Why? to offer the possibility to override the configuration in a programmatic way
    @ConditionalOnMissingBean(OutboxConfiguration.class)
    public OutboxConfiguration  outboxConfiguration(
        DataProvider dataProvider,
        LockingProvider lockingProvider,
        OutboxProperties transactionalOutboxProperties,
        SchedulingProvider schedulingProvider,
        SerializationProvider serializationProvider,
        OutboxItemHandlerProvider outboxItemHandlerProvider,
        TransactionManagerProvider transactionManagerProvider
    ){
        OutboxConfiguration outboxConfiguration = OutboxConfiguration.builder()
            .fixedDelay(transactionalOutboxProperties.getFixedDelay())
            .maxAttempts(transactionalOutboxProperties.getMaxAttempts())
            .backoffBase(transactionalOutboxProperties.getBackoffBase())
            .dataProvider(dataProvider)
            .schedulingProvider(schedulingProvider)
            .serializationProvider(serializationProvider)
            .lockingProvider(lockingProvider)
            .outboxItemHandlerProvider(outboxItemHandlerProvider)
            .transactionManagerProvider(transactionManagerProvider)
            .build();
        logger.debug("Transactional Outbox Starter configuration details: {}",outboxConfiguration);
        return outboxConfiguration;
    }

    // that's the bean that implements the processor that will handle the outbox items
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnBean({
        OutboxConfiguration.class
    })
    public OutboxProcessor transactionalOutboxProcessor(OutboxConfiguration outboxConfiguration){
        return OutboxProcessorFactory.createOutboxProcessor(outboxConfiguration);
    }



    // That's bean that activates the outbox scheduler(see core module)
    @Bean
    @Conditional({
        StarterEnabled.class,
        ContextValidCondition.class
    })
    @ConditionalOnBean({
        OutboxConfiguration.class,
        OutboxProcessor.class
    })
    public OutboxScheduler transactionalOutboxScheduler(
        OutboxConfiguration outboxConfiguration,
        OutboxProcessor outboxProcessor
    ){

        return OutboxSchedulerFactory.createOutboxScheduler(outboxConfiguration, outboxProcessor);
    }

}

package it.gov.acn.autoconfigure.outbox;

import it.gov.acn.autoconfigure.outbox.config.OutboxProperties;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

public class ContextRunnerDecorator {
    private ApplicationContextRunner contextRunner;
    public static ContextRunnerDecorator create(ApplicationContextRunner contextRunner) {
        return new ContextRunnerDecorator(contextRunner);
    }
    private ContextRunnerDecorator(ApplicationContextRunner contextRunner) {
        this.contextRunner = contextRunner;
    }

    public ContextRunnerDecorator withTaskScheduler() {
        this.contextRunner = this.contextRunner.withBean("testTaskScheduler", TaskScheduler.class,
                () ->  Mockito.mock(ThreadPoolTaskScheduler.class));
        return this;
    }

    public ContextRunnerDecorator withEnabled(boolean enabled) {
        this.contextRunner = this.contextRunner.withPropertyValues(OutboxProperties.EnvPropertyKeys.ENABLED.getKeyWithPrefix() + "=" + enabled);
        return this;
    }

    public ContextRunnerDecorator withFixedDelay(long fixedDelay) {
        this.contextRunner = this.contextRunner.withPropertyValues(OutboxProperties.EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix() + "=" + fixedDelay);
        return this;
    }

    public ContextRunnerDecorator withDatasource() {
        DataSource postgresDataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);

        try {
            Mockito.when(connection.getMetaData()).thenReturn(metaData);
            Mockito.when(postgresDataSource.getConnection()).thenReturn(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.contextRunner = this.contextRunner.withBean("dataSource", DataSource.class, () -> postgresDataSource);
        return this;
    }

    public ContextRunnerDecorator withTransactionManager() {
        PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);

        this.contextRunner = this.contextRunner.withBean("transactionManager", PlatformTransactionManager.class, () -> transactionManager);
        return this;
    }

    public ContextRunnerDecorator withOutboxItemHandlerProvider() {
        OutboxItemHandlerProvider outboxItemHandlerProvider = Mockito.mock(OutboxItemHandlerProvider.class);

        this.contextRunner = this.contextRunner.withBean("outboxItemHandlerProvider",
                OutboxItemHandlerProvider.class, () -> outboxItemHandlerProvider);
        return this;
    }

    public ApplicationContextRunner claim() {
        return this.contextRunner;
    }
}
import it.gov.acn.config.TransactionalOutboxProperties;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class ContextRunnerDecorator {
    private ApplicationContextRunner contextRunner;
    public static ContextRunnerDecorator create(ApplicationContextRunner contextRunner) {
        return new ContextRunnerDecorator(contextRunner);
    }
    private ContextRunnerDecorator(ApplicationContextRunner contextRunner) {
        this.contextRunner = contextRunner;
    }

    public ContextRunnerDecorator withUserConfiguration(Class<?>... userConfiguration) {
        this.contextRunner = this.contextRunner.withUserConfiguration(userConfiguration);
        return this;
    }

    public ContextRunnerDecorator withEnabled(boolean enabled) {
        this.contextRunner = this.contextRunner.withPropertyValues(TransactionalOutboxProperties.EnvPropertyKeys.ENABLED.getKeyWithPrefix() + "=" + enabled);
        return this;
    }

    public ContextRunnerDecorator withFixedDelay(long fixedDelay) {
        this.contextRunner = this.contextRunner.withPropertyValues(TransactionalOutboxProperties.EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix() + "=" + fixedDelay);
        return this;
    }

    public ContextRunnerDecorator withPostgres() {
        DataSource postgresDataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);

        try {
            Mockito.when(connection.getMetaData()).thenReturn(metaData);
            Mockito.when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/test");
            Mockito.when(postgresDataSource.getConnection()).thenReturn(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.contextRunner = this.contextRunner.withBean("dataSource", DataSource.class, () -> postgresDataSource);
        return this;
    }

    public ApplicationContextRunner claim() {
        return this.contextRunner;
    }
}
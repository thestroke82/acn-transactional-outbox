package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.IntegrationTestConfig;
import it.gov.acn.autoconfigure.outbox.TestApplication;
import it.gov.acn.autoconfigure.outbox.providers.postgres.PostgresJdbcDataProvider;
import it.gov.acn.outboxprocessor.model.DataProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;


@Disabled("This test is not working. Integration testing are diverted to the acn-transactional-outbox-example-app module.")
@SpringBootTest
@ContextConfiguration(classes = TestApplication.class)
@Import(IntegrationTestConfig.class)
public class OutboxAutoconfigurationIntegrationTests{

    @Autowired
    private DataProvider dataProvider;

    @Test
    public void testDataProvider(){
        assert dataProvider instanceof PostgresJdbcDataProvider;
    }
}

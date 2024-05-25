package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.IntegrationTestConfig;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;


@Disabled("Not used for now")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class OutboxAutoconfigurationIntegrationTests{


}

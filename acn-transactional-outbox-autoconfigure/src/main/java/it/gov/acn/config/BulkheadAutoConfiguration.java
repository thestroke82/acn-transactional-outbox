package it.gov.acn.config;

import it.gov.acn.config.TransactionalOutboxProperties.EnvPropertyKeys;
import it.gov.acn.etc.PropertiesHelper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;


@AutoConfiguration
public class BulkheadAutoConfiguration {
  @Bean
  public TransactionalOutboxProperties transactionalOutboxProperties(
      Environment environment
  ) {

    // This is a less than ideal solution because the properties are not being loaded into the context.
    // This is due to the ConfigurationPhase.REGISTRATION_BEAN phase (refer to ContextValidCondition for more details).

    TransactionalOutboxProperties properties = new TransactionalOutboxProperties();

    Optional<Boolean> enabled = PropertiesHelper.getBooleanProperty(
        EnvPropertyKeys.ENABLED.getKeyWithPrefix(), environment);
    enabled.ifPresent(properties::setEnabled);
    Optional<Long> fixedDelay = PropertiesHelper.getLongProperty(
        EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix(), environment);
    fixedDelay.ifPresent(properties::setFixedDelay);
    Optional<String> tableName = PropertiesHelper.getStringProperty(
        EnvPropertyKeys.TABLE_NAME.getKeyWithPrefix(), environment);
    tableName.ifPresent(properties::setTableName);

    return properties;
  }
}

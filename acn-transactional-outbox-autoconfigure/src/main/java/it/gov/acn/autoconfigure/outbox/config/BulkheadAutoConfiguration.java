package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.config.OutboxProperties.EnvPropertyKeys;
import it.gov.acn.autoconfigure.outbox.etc.PropertiesHelper;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;


@AutoConfiguration
public class BulkheadAutoConfiguration {
  @Bean
  public OutboxProperties transactionalOutboxProperties(
      Environment environment
  ) {

    // This is a less than ideal solution because the properties are not being loaded into the context.
    // This is due to the ConfigurationPhase.REGISTRATION_BEAN phase (refer to ContextValidCondition for more details).

    OutboxProperties properties = new OutboxProperties();

    Optional<Boolean> enabled = PropertiesHelper.getBooleanProperty(
        EnvPropertyKeys.ENABLED.getKeyWithPrefix(), environment);
    enabled.ifPresent(properties::setEnabled);
    Optional<Long> fixedDelay = PropertiesHelper.getLongProperty(
        EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix(), environment);
    fixedDelay.ifPresent(properties::setFixedDelay);
    Optional<String> tableName = PropertiesHelper.getStringProperty(
        EnvPropertyKeys.TABLE_NAME.getKeyWithPrefix(), environment);
    tableName.ifPresent(properties::setTableName);
    Optional<Integer> backoffBase = PropertiesHelper.getIntegerProperty(
        EnvPropertyKeys.BACKOFF_BASE.getKeyWithPrefix(), environment);
    backoffBase.ifPresent(properties::setBackoffBase);

    return properties;
  }
}

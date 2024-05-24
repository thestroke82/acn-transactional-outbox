package it.gov.acn.autoconfigure.outbox.condition;

import it.gov.acn.autoconfigure.outbox.config.OutboxProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class StarterEnabled implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Boolean enabled = context.getEnvironment()
        .getProperty(OutboxProperties.EnvPropertyKeys.ENABLED.getKeyWithPrefix(),Boolean.class);
    return enabled != null && enabled;
  }
}

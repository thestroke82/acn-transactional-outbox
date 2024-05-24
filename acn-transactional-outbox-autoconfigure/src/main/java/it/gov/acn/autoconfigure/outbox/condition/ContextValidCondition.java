package it.gov.acn.autoconfigure.outbox.condition;

import it.gov.acn.autoconfigure.outbox.condition.requirement.ContextRequirementsValidator;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ContextValidCondition implements ConfigurationCondition {
  @Override
  public ConfigurationPhase getConfigurationPhase() {
    return ConfigurationPhase.REGISTER_BEAN;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ContextRequirementsValidator validator = ContextRequirementsValidator.getInstance(context);
    return validator.validate();
  }
}

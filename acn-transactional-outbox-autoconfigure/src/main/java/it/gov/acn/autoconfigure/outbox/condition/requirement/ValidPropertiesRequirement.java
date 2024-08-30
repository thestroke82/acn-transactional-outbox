package it.gov.acn.autoconfigure.outbox.condition.requirement;

import it.gov.acn.autoconfigure.outbox.config.OutboxProperties;
import it.gov.acn.autoconfigure.outbox.etc.PropertiesHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;

public class ValidPropertiesRequirement implements ContextRequirement {

  private final List<String> validationErrorMessages = new ArrayList<>();
  private final Environment environment;

  public ValidPropertiesRequirement(Environment environment) {
    this.environment = environment;
  }

  @Override
  public boolean isSatisfied() {
    validateFixedDelay();
    return validationErrorMessages.isEmpty();
  }

  private void validateFixedDelay() {
    Optional<Long> fixedDelay =
        PropertiesHelper.getLongProperty(OutboxProperties.EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix(), environment);
    if (fixedDelay.isPresent() && fixedDelay.get() < 100) {
      validationErrorMessages.add("Fixed delay must be at least 100 milliseconds");
    }
  }


  @Override
  public Optional<String> getProblem() {
    return Optional.of(String.join(", ", validationErrorMessages));
  }
}

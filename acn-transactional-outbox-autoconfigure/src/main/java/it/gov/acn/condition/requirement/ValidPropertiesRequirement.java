package it.gov.acn.condition.requirement;

import it.gov.acn.config.TransactionalOutboxProperties;
import it.gov.acn.etc.PropertiesHelper;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                PropertiesHelper.getLongProperty(TransactionalOutboxProperties.EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix(), environment);
        if(fixedDelay.isPresent() && fixedDelay.get() < 100) {
            validationErrorMessages.add("Fixed delay must be at least 100 milliseconds");
        }
    }



    @Override
    public Optional<String> getProblem() {
        return Optional.of(String.join(", ", validationErrorMessages));
    }
}

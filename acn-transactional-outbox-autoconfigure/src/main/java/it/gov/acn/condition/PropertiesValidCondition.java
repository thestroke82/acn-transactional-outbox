package it.gov.acn.condition;

import it.gov.acn.config.ErrorMessagesHolder;
import it.gov.acn.config.TransactionalOutboxProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PropertiesValidCondition implements Condition {
    private List<String> validationErrorMessages = new ArrayList<>();
    private Environment environment;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        this.environment = context.getEnvironment();
        validateFixedDelay();
        validationErrorMessages.forEach(ErrorMessagesHolder::addErrorMessage);
        return validationErrorMessages.isEmpty();
    }

    private void validateFixedDelay() {
        Optional<Long> fixedDelay =
                getLongProperty(TransactionalOutboxProperties.EnvPropertyKeys.FIXED_DELAY.getKeyWithPrefix());
        if(fixedDelay.isPresent() && fixedDelay.get() < 100) {
            validationErrorMessages.add("Fixed delay must be at least 100 milliseconds");
        }
    }

    private Optional<Boolean> getBooleanProperty(String key) {
        String property = this.environment.getProperty(key);
        return Optional.ofNullable(property).map(Boolean::parseBoolean);
    }

    private Optional<Long> getLongProperty(String key) {
        String property = this.environment.getProperty(key);
        try {
            return Optional.ofNullable(property).map(Long::parseLong);
        } catch (NumberFormatException e) {
            validationErrorMessages.add("Invalid value for property " + key);
            return Optional.empty();
        }
    }
}

package it.gov.acn.autoconfigure.outbox.condition.requirement;

import it.gov.acn.autoconfigure.outbox.etc.Utils;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Optional;

public class OutboxItemHandlerRequirement implements  ContextRequirement{
    private final ConfigurableListableBeanFactory beanFactory;

    public OutboxItemHandlerRequirement(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean isSatisfied() {
        boolean ret = Utils.isBeanPresentInContext(beanFactory, OutboxItemHandlerProvider.class);
        return ret;
    }

    @Override
    public Optional<String> getProblem() {
        return Optional.of("No OutboxItemHandler found in context");
    }
}

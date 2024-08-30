package it.gov.acn.autoconfigure.outbox.condition.requirement;

import it.gov.acn.autoconfigure.outbox.etc.Utils;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

public class TransactionManagerRequirement implements ContextRequirement {

  private final ConfigurableListableBeanFactory beanFactory;

  public TransactionManagerRequirement(ConfigurableListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean isSatisfied() {
    return Utils.isBeanPresentInContext(beanFactory, PlatformTransactionManager.class);
  }

  @Override
  public Optional<String> getProblem() {
    return Optional.of("No PlatformTransactionManager found in context");
  }
}

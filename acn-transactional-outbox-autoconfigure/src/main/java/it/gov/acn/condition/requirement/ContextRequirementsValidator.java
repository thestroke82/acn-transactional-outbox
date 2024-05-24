package it.gov.acn.condition.requirement;

import it.gov.acn.config.TransactionalOutboxProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

/**
 * This class is responsible for validating the context requirements for the Transactional Outbox Starter.
 * Conditions:
 *  - The properties are valid
 *  - A DataSource is present
 *  - A TransactionManager is present
 *  - The outbox table is present and valid
 */
public class ContextRequirementsValidator {

  private final Logger logger = LoggerFactory.getLogger(ContextRequirementsValidator.class);

  private static ContextRequirementsValidator instance;
  private final List<ContextRequirement> requirements;
  private ConditionContext context;
  private TransactionalOutboxProperties properties;

  private Boolean valid;

  private ContextRequirementsValidator(ConditionContext context) {
    this.context = context;
    this.properties = Objects.requireNonNull(context.getBeanFactory()).getBean(TransactionalOutboxProperties.class);
    Environment environment = context.getEnvironment();
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

    this.requirements = List.of(
        new ValidPropertiesRequirement(environment),
        new DataSourceRequirement(beanFactory),
        new TransactionManagerRequirement(beanFactory),
        new OutboxTableRequirement(beanFactory, properties)
    );
  }

  public static ContextRequirementsValidator getInstance(ConditionContext context) {
    if (instance == null) {
      instance = new ContextRequirementsValidator(context);
    }
    return instance;
  }

  public boolean validate() {
    if(valid!=null){
      return valid;
    }
    List<String> problems = new ArrayList<>();
    for (ContextRequirement requirement : requirements) {
      boolean isSatisfied = requirement.isSatisfied();
      if (!isSatisfied && requirement.getProblem().isPresent()) {
        problems.add(requirement.getProblem().get());
      }
    }
    valid = problems.isEmpty();
    if(!valid){
      StringBuilder message = new StringBuilder("Transactional Outbox Starter configuration is not valid: ");
      problems.forEach(problem -> message.append(problem).append(", "));
      logger.error(message.toString());
    }
    return valid;
  }
}
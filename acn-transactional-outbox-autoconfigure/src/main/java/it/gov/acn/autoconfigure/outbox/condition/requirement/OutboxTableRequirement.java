package it.gov.acn.autoconfigure.outbox.condition.requirement;

import it.gov.acn.autoconfigure.outbox.config.OutboxProperties;
import it.gov.acn.autoconfigure.outbox.etc.Utils;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Was supposed to check if the outbox table exists in the database. It is not active at the moment. probably never will
 * be. Consider removing it.
 */
public class OutboxTableRequirement implements ContextRequirement {

  private final Logger logger = LoggerFactory.getLogger(OutboxTableRequirement.class);
  private final ConfigurableListableBeanFactory beanFactory;
  private final OutboxProperties properties;

  public OutboxTableRequirement(
      ConfigurableListableBeanFactory beanFactory,
      OutboxProperties properties
  ) {
    this.beanFactory = beanFactory;
    this.properties = properties;
  }

  @Override
  public boolean isSatisfied() {
    try {
      DataSource dataSource = beanFactory.getBean(DataSource.class);
      return Utils.doesTableExist(dataSource, properties.getTableName());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<String> getProblem() {
    return Optional.of("Outbox table '" + this.properties.getTableName() + "' not found");
  }
}

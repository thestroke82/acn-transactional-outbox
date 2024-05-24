package it.gov.acn.condition.requirement;

import it.gov.acn.config.TransactionalOutboxProperties;
import it.gov.acn.etc.Utils;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class OutboxTableRequirement implements ContextRequirement{
  private final Logger logger = LoggerFactory.getLogger(OutboxTableRequirement.class);
  private final ConfigurableListableBeanFactory beanFactory;
  private final TransactionalOutboxProperties properties;

  public OutboxTableRequirement(
      ConfigurableListableBeanFactory beanFactory,
      TransactionalOutboxProperties properties
  ) {
    this.beanFactory = beanFactory;
    this.properties = properties;
  }

  @Override
  public boolean isSatisfied() {
    try {
      DataSource dataSource = (DataSource) beanFactory.getBean(DataSource.class);
      return Utils.doesTableExist(dataSource, properties.getTableName());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<String> getProblem() {
    return Optional.of("Outbox table '" +this.properties.getTableName()+ "' not found");
  }
}

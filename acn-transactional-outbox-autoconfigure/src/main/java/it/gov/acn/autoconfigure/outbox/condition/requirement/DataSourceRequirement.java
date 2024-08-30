package it.gov.acn.autoconfigure.outbox.condition.requirement;

import it.gov.acn.autoconfigure.outbox.etc.Utils;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class DataSourceRequirement implements ContextRequirement {

  private final ConfigurableListableBeanFactory beanFactory;

  public DataSourceRequirement(ConfigurableListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public DataSourceRequirement() {
    this.beanFactory = null;
  }

  @Override
  public boolean isSatisfied() {
    boolean ret = Utils.isBeanPresentInContext(beanFactory, DataSource.class);
    return ret;
  }

  @Override
  public Optional<String> getProblem() {
    return Optional.of("No DataSource found in context");
  }

}

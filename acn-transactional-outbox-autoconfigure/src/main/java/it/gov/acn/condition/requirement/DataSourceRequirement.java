package it.gov.acn.condition.requirement;

import it.gov.acn.etc.Utils;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import javax.sql.DataSource;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DataSourceRequirement implements ContextRequirement{

    private final ConfigurableListableBeanFactory beanFactory;

    public DataSourceRequirement(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public DataSourceRequirement () {
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

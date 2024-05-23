package it.gov.acn.condition;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.sql.DataSource;

public class DataSourceInContextCondition implements ConfigurationCondition {

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String[] beanNamesForType = context.getBeanFactory().getBeanNamesForType(DataSource.class, true, false);
        return beanNamesForType!=null && beanNamesForType.length>0;
    }
}

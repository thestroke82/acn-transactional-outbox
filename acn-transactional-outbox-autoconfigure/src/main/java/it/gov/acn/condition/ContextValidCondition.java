package it.gov.acn.condition;

import it.gov.acn.context.ContextBulkhead;
import it.gov.acn.context.ValidContextBulkhead;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ContextValidCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Object contextValidator =
                context.getBeanFactory().getBean(ContextBulkhead.class);
        return contextValidator instanceof ValidContextBulkhead;
    }
}

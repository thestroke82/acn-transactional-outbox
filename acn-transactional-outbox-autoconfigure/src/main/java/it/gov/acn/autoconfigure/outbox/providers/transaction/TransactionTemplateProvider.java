package it.gov.acn.autoconfigure.outbox.providers.transaction;

import it.gov.acn.outbox.provider.TransactionManagerProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

public class TransactionTemplateProvider implements TransactionManagerProvider {

    private TransactionTemplate transactionTemplate;


    public TransactionTemplateProvider(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    /**
     * This is the programmatic equivalent of @Transactional annotation
     * @return
     */

    @Override
    public void executeInTransaction(Runnable runnable) {
        Assert.notNull(runnable, "Runnable must not be null");
        this.transactionTemplate.execute(status -> {
            runnable.run();
            return null;
        });
    }
}
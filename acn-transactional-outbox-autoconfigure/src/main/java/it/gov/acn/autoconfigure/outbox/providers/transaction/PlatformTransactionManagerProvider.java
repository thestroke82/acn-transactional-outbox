package it.gov.acn.autoconfigure.outbox.providers.transaction;

import it.gov.acn.outbox.provider.TransactionManagerProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class PlatformTransactionManagerProvider implements TransactionManagerProvider {

    private PlatformTransactionManager transactionManager;

    private final ThreadLocal<TransactionStatus> currentTransaction = new ThreadLocal<>();

    public PlatformTransactionManagerProvider(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * This is the programmatic equivalent of @Transactional annotation
     * @return
     */
    @Override
    public TransactionStatus beginTransaction() {
        TransactionStatus existingTransaction = getCurrentTransaction();
        if (existingTransaction != null) {
            // If there is already a transaction, return it
            return existingTransaction;
        }
        
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("outboxTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(def);
        currentTransaction.set(status);
        return status;
    }

    @Override
    public void commit() {
        TransactionStatus status = getCurrentTransaction();
        if (status != null) {
            transactionManager.commit(status);
            currentTransaction.remove();
        }
    }

    @Override
    public void rollback() {
        TransactionStatus status = getCurrentTransaction();
        if (status != null) {
            transactionManager.rollback(status);
            currentTransaction.remove();
        }
    }

    private TransactionStatus getCurrentTransaction() {
        return currentTransaction.get();
    }
}
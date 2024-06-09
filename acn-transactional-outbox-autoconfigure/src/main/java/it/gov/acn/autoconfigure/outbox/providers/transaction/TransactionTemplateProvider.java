package it.gov.acn.autoconfigure.outbox.providers.transaction;

import it.gov.acn.outbox.provider.TransactionManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class TransactionTemplateProvider implements TransactionManagerProvider {
    private Logger logger = LoggerFactory.getLogger(TransactionTemplateProvider.class);
    @Override
    @Transactional
    public void executeInTransaction(Runnable runnable) {
        this.logTransactionStatus("Beginning of template");
        Assert.notNull(runnable, "Runnable must not be null");
        runnable.run();
    }

     private void logTransactionStatus(String phase) {
        logger.trace("[{}] Transaction active: {}", phase, TransactionSynchronizationManager.isActualTransactionActive());
        logger.trace("[{}] Synchronization active: {}", phase, TransactionSynchronizationManager.isSynchronizationActive());
        logger.trace("[{}] Current transaction name: {}", phase, TransactionSynchronizationManager.getCurrentTransactionName());
        logger.trace("[{}] Current transaction isolation level: {}", phase, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
        logger.trace("[{}] Current transaction read-only: {}", phase, TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }
}
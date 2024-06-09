package it.gov.acn.autoconfigure.outbox.providers.transaction;

import it.gov.acn.outbox.provider.TransactionManagerProvider;
import org.slf4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

public class TransactionTemplateProvider implements TransactionManagerProvider {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(TransactionTemplateProvider.class);
    @Override
    @Transactional
    public void executeInTransaction(Runnable runnable) {
        this.logTransactionStatus("Beginning of template");
        Assert.notNull(runnable, "Runnable must not be null");
        runnable.run();
    }

     private void logTransactionStatus(String phase) {
        logger.info("[{}] Transaction active: {}", phase, TransactionSynchronizationManager.isActualTransactionActive());
        logger.info("[{}] Synchronization active: {}", phase, TransactionSynchronizationManager.isSynchronizationActive());
        logger.info("[{}] Current transaction name: {}", phase, TransactionSynchronizationManager.getCurrentTransactionName());
        logger.info("[{}] Current transaction isolation level: {}", phase, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
        logger.info("[{}] Current transaction read-only: {}", phase, TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }
}
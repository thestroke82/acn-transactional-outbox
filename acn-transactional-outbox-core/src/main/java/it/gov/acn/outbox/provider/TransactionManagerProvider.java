package it.gov.acn.outbox.provider;

public interface TransactionManagerProvider {
  void executeInTransaction(Runnable runnable);
}

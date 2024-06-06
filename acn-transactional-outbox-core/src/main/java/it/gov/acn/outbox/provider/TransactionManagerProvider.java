package it.gov.acn.outbox.provider;

public interface TransactionManagerProvider {

  Object beginTransaction();

  void commit();

  void rollback();
}

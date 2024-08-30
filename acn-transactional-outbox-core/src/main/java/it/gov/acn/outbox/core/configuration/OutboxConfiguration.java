package it.gov.acn.outbox.core.configuration;

import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.LockingProvider;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;
import it.gov.acn.outbox.provider.SchedulingProvider;
import it.gov.acn.outbox.provider.SerializationProvider;
import it.gov.acn.outbox.provider.TransactionManagerProvider;

public class OutboxConfiguration {

  /**
   * The delay between each poll of the outbox table.
   */
  private long fixedDelay;

  /**
   * The maximum number of attempts to process an item from the outbox, after which it will no longer be considered for
   * processing.
   */
  private int maxAttempts;

  /**
   * The base value for the backoff calculation, in minutes. Example with backoffBase=5 and maxAttempts=4: - first
   * attempt: as soon as the scheduler runs - second attempt: 5 minutes after the first failed attempt - third attempt:
   * 25 minutes after the second failed attempt - fourth attempt: 125 minutes after the third failed attempt
   */
  private int backoffBase;

  /**
   * The data provider to use for fetching and updating outbox items.
   */
  private DataProvider dataProvider;

  /**
   * The scheduling provider to use for scheduling the outbox task.
   */
  private SchedulingProvider schedulingProvider;

  /**
   * The serialization provider to use for serializing events to JSON.
   */
  private SerializationProvider serializationProvider;

  /**
   * The locking provider to use for locking outbox processing. So if multiple instances of the application are running,
   * only one will process the outbox at a time.
   */
  private LockingProvider lockingProvider;


  /**
   * The outbox item handler provider to use for handling outbox items.
   */
  private OutboxItemHandlerProvider outboxItemHandlerProvider;

  /**
   * The transaction manager provider to use for handling transactions.
   */
  private TransactionManagerProvider transactionManagerProvider;


  private OutboxConfiguration() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long fixedDelay;
    private int maxAttempts;
    private int backoffBase;
    private DataProvider dataProvider;
    private SchedulingProvider schedulingProvider;
    private SerializationProvider serializationProvider;
    private LockingProvider lockingProvider;
    private OutboxItemHandlerProvider outboxItemHandlerProvider;

    private TransactionManagerProvider transactionManagerProvider;

    public Builder fixedDelay(long fixedDelay) {
      this.fixedDelay = fixedDelay;
      return this;
    }

    public Builder maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
    }

    public Builder backoffBase(int backoffBase) {
      this.backoffBase = backoffBase;
      return this;
    }

    public Builder dataProvider(DataProvider dataProvider) {
      this.dataProvider = dataProvider;
      return this;
    }

    public Builder schedulingProvider(SchedulingProvider schedulingProvider) {
      this.schedulingProvider = schedulingProvider;
      return this;
    }

    public Builder serializationProvider(SerializationProvider serializationProvider) {
      this.serializationProvider = serializationProvider;
      return this;
    }

    public Builder lockingProvider(LockingProvider lockingProvider) {
      this.lockingProvider = lockingProvider;
      return this;
    }

    public Builder outboxItemHandlerProvider(OutboxItemHandlerProvider outboxItemHandlerProvider) {
      this.outboxItemHandlerProvider = outboxItemHandlerProvider;
      return this;
    }

    public Builder transactionManagerProvider(TransactionManagerProvider transactionManagerProvider) {
      this.transactionManagerProvider = transactionManagerProvider;
      return this;
    }

    public OutboxConfiguration build() {
      OutboxConfiguration config = new OutboxConfiguration();
      config.fixedDelay = this.fixedDelay;
      config.maxAttempts = this.maxAttempts;
      config.backoffBase = this.backoffBase;
      config.dataProvider = this.dataProvider;
      config.schedulingProvider = this.schedulingProvider;
      config.serializationProvider = this.serializationProvider;
      config.lockingProvider = this.lockingProvider;
      config.outboxItemHandlerProvider = this.outboxItemHandlerProvider;
      config.transactionManagerProvider = this.transactionManagerProvider;
      return config;
    }
  }

  public long getFixedDelay() {
    return fixedDelay;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getBackoffBase() {
    return backoffBase;
  }

  public DataProvider getDataProvider() {
    return dataProvider;
  }

  public SchedulingProvider getSchedulingProvider() {
    return schedulingProvider;
  }

  public SerializationProvider getSerializationProvider() {
    return serializationProvider;
  }

  public LockingProvider getLockingProvider() {
    return lockingProvider;
  }

  public OutboxItemHandlerProvider getOutboxItemHandlerProvider() {
    return outboxItemHandlerProvider;
  }

  public TransactionManagerProvider getTransactionManagerProvider() {
    return transactionManagerProvider;
  }

  @Override
  public String toString() {
    return "OutboxConfiguration{" +
           "fixedDelay=" + fixedDelay +
           ", maxAttempts=" + maxAttempts +
           ", backoffBase=" + backoffBase +
           ", dataProvider=" + dataProvider.getClass().getSimpleName() +
           ", schedulingProvider=" + schedulingProvider.getClass().getSimpleName() +
           ", serializationProvider=" + serializationProvider.getClass().getSimpleName() +
           ", outboxItemHandlerProvider=" + outboxItemHandlerProvider.getClass().getSimpleName() +
           ", lockingProvider=" + lockingProvider.getClass().getSimpleName() +
           ", transactionManagerProvider=" + transactionManagerProvider.getClass().getSimpleName() +
           '}';
  }
}

package it.gov.acn.outbox.core;

import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.SchedulingProvider;

public class OutboxConfiguration {

    /**
     * The delay between each poll of the outbox table.
     */
    private long fixedDelay;

    /**
     * The maximum number of attempts to process an item from the outbox, after which it will
     * no longer be considered for processing.
     */
    private int maxAttempts;

    /**
     * The base value for the backoff calculation, in seconds.
     * Example with backoffBase=5*60(5 minutes) and maxAttempts=4:
     *  - first attempt: as soon as the scheduler runs
     *  - second attempt: 5 minutes after the first failed attempt
     *  - third attempt: 25 minutes after the second failed attempt
     *  - fourth attempt: 125 minutes after the third failed attempt
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

       public OutboxConfiguration build() {
            OutboxConfiguration config = new OutboxConfiguration();
            config.fixedDelay = this.fixedDelay;
            config.maxAttempts = this.maxAttempts;
            config.backoffBase = this.backoffBase;
            config.dataProvider = this.dataProvider;
            config.schedulingProvider = this.schedulingProvider;
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

    @Override
    public String toString() {
        return "OutboxProcessorConfiguration{" +
                "fixedDelay=" + fixedDelay +
                ", maxAttempts=" + maxAttempts +
                ", backoffBase=" + backoffBase +
                ", dataProvider=" + dataProvider +
                ", schedulingProvider=" + schedulingProvider +
                '}';
    }
}

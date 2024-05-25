package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.etc.Constants;

public class OutboxProperties {
    public enum EnvPropertyKeys {
        ENABLED("enabled"),
        FIXED_DELAY("fixed-delay"),
        TABLE_NAME("table-name"),
        MAX_ATTEMPTS("max-attempts"),
        BACKOFF_BASE("backoff-base");

        private final String key;

        EnvPropertyKeys(String key) {
            this.key = key;
        }
        public String getKey() {
            return this.key;
        }
        public String getKeyWithPrefix() {
            return Constants.APP_PROPERTIES_PREFIX + "." + this.key;
        }
    }
    /**
     * Enable or disable the outbox scheduler.
     */
    private boolean enabled = DefaultConfiguration.ENABLED;

    /**
     * Fixed delay in milliseconds between the end of the last invocation and the start of the next.
     */
    private long fixedDelay = DefaultConfiguration.FIXED_DELAY;

    /**
     * Table name for the outbox.
     */
    private String tableName = DefaultConfiguration.TABLE_NAME;

    /**
     * The maximum number of attempts to process an item from the outbox, after which it will
     * no longer be considered for processing.
     */
    private int maxAttempts = DefaultConfiguration.MAX_ATTEMPTS;

    /**
     * The base value for the backoff calculation, in seconds.
     * Example with backoffBase=5*60(5 minutes) and maxAttempts=4:
     *  - first attempt: as soon as the scheduler runs
     *  - second attempt: 5 minutes after the first failed attempt
     *  - third attempt: 25 minutes after the second failed attempt
     *  - fourth attempt: 125 minutes after the third failed attempt
     */
    private int backoffBase = DefaultConfiguration.BACKOFF_BASE;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBackoffBase() {
        return backoffBase;
    }

    public void setBackoffBase(int backoffBase) {
        this.backoffBase = backoffBase;
    }

    @Override
    public String toString() {
        return "OutboxProperties{" +
                "enabled=" + enabled +
                ", fixedDelay=" + fixedDelay +
                ", tableName='" + tableName + '\'' +
                ", maxAttempts=" + maxAttempts +
                ", backoffBase=" + backoffBase +
                '}';
    }
}

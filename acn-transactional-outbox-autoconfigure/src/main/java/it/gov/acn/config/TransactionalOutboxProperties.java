package it.gov.acn.config;

import it.gov.acn.etc.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.stereotype.Component;

public class TransactionalOutboxProperties {
    public enum EnvPropertyKeys {
        ENABLED("enabled"),
        FIXED_DELAY("fixed-delay"),
        TABLE_NAME("table-name");

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

    @Override
    public String toString() {
        return "TransactionalOutboxProperties{" +
                "enabled=" + enabled +
                ", fixedDelay=" + fixedDelay +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}

package it.gov.acn.autoconfigure.outbox.config;

public class DefaultConfiguration {
    public static final boolean ENABLED = false;
    public static final long FIXED_DELAY = 5000; // milliseconds
    public static final String TABLE_NAME = "transactional_outbox";

    public static final int MAX_ATTEMPTS = 3;

    public static final int BACKOFF_BASE = 5*60; // 5 minutes, it's in seconds
}
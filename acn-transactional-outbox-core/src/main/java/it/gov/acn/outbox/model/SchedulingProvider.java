package it.gov.acn.outbox.model;

import java.util.concurrent.ScheduledFuture;

public interface SchedulingProvider {
    ScheduledFuture<?> schedule(Runnable task, long delay);
}

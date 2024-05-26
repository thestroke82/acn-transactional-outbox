package it.gov.acn.autoconfigure.outbox.providers.scheduling;

import it.gov.acn.outbox.model.SchedulingProvider;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;

public class TaskSchedulerSchedulingProvider implements SchedulingProvider {
    private TaskScheduler taskScheduler;

    public TaskSchedulerSchedulingProvider(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }
    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay) {
        return this.taskScheduler.scheduleWithFixedDelay(task, Duration.of(delay, ChronoUnit.MILLIS));
    }
}

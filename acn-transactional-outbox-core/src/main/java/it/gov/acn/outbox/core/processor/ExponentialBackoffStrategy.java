package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;
import java.time.Duration;
import java.time.Instant;

public class ExponentialBackoffStrategy implements OutboxItemSelectionStrategy {

  private final int backoffBase;

  public ExponentialBackoffStrategy(int backoffBase) {
    this.backoffBase = backoffBase;
  }

  @Override
  public boolean filter(OutboxItem item) {

    Instant now = Instant.now();

    // outbox items that have never been attempted are always accepted
    if (item.getAttempts() == 0 || item.getLastAttemptDate() == null) {
      return true;
    }

    // accepting only outbox for which the current backoff period has expired
    // the backoff period is calculated as base^attempts
    Instant backoffProjection = item.getLastAttemptDate()
        .plus(Duration.ofMinutes((long) Math.pow(backoffBase, item.getAttempts())));

    // if the projection is before now, it's time to retry, i.e. the backoff period has expired
    return backoffProjection.isBefore(now);
  }
}
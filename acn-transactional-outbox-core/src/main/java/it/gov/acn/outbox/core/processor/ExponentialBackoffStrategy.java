package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ExponentialBackoffStrategy implements OutboxItemSelectionStrategy{
    private final int backoffBase;

    public ExponentialBackoffStrategy(int backoffBase) {
        this.backoffBase = backoffBase;
    }

     /**
      * Select outbox items that have not yet been attempted or have completed the exponential backoff period
      * @param outstandingItems list of outbox items to filter
     *                    @return filtered list of outbox items
     */
    @Override
    public List<OutboxItem> execute(List<OutboxItem> outstandingItems) {
        if(outstandingItems==null || outstandingItems.isEmpty()){
            return outstandingItems;
        }
        Instant now = Instant.now();
        return outstandingItems.stream().filter(oe->{
            // outbox items that have never been attempted are always accepted
            if(oe.getAttempts()==0 ||  oe.getLastAttemptDate()==null){
                return true;
            }
            // accepting only outbox for which the current backoff period has expired
            // the backoff period is calculated as base^attempts
            Instant backoffProjection = oe.getLastAttemptDate()
                    .plus(Duration.ofMinutes((long) Math.pow(backoffBase, oe.getAttempts())));

            // if the projection is before now, it's time to retry, i.e. the backoff period has expired
            return backoffProjection.isBefore(now);
        }).toList();
    }
}
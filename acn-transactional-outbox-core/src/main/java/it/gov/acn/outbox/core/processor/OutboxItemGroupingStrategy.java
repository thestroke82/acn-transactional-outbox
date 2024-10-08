package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;
import java.util.List;

public interface OutboxItemGroupingStrategy {

  /**
   * Group outbox items based on the concrete implementation of a strategy
   *
   * @param outstandingItems list of outbox items to group, sorted by creation date ascending
   * @return filtered list of outbox items
   */
  List<List<OutboxItem>> group(List<OutboxItem> outstandingItems);
}
package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;

import java.util.List;

public interface OutboxItemSelectionStrategy {
    /**
      * Select outbox items based on the concrete implementation of a strategy
      * @param item the item to select
     *  @return true to include the item in the processing pipeline
     */
    boolean filter(OutboxItem item);

    /**
     * Just a helper to work with a list of items
     * @param items A list of items, sorted by creation date ascending
     * @return A list of filtered items
     */
    default List<OutboxItem> filter(List<OutboxItem> items) {
        return items == null ? null : items.stream().filter(this::filter).toList();
    }
}
package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A grouping strategy that will pick the first (i.e. oldest) item in each
 * group and will pick ALL the item in the null group.
 * A group is defined by the group ID, items with the same group ID will be in
 * the same group.
 * The null group is the group whose group ID is null.
 */
public class NullableIdGroupingStrategy implements OutboxItemGroupingStrategy {

    @Override
    public List<List<OutboxItem>> group(List<OutboxItem> outstandingItems) {
        //Found a test requiring this
        if (outstandingItems == null) {
            return null;
        }

        //Map from each group ID to a list of items
        //Stupid groupingBy collector requires non-null keys
        var itemsByGroupId = outstandingItems
                .stream()
                .collect(Collectors.groupingBy(item -> Optional.ofNullable(item.getGroupId())));

        //All the items with a null group ID are a group by themselves
        var itemsWithoutGroupId = itemsByGroupId.getOrDefault(Optional.<String>empty(), List.of())
                .stream()
                .map(List::of)
                .toList();

        //Remove the null group
        itemsByGroupId.remove(Optional.<String>empty());

        //The concatenation of the two lists (if not empty)
        var result = new ArrayList<List<OutboxItem>>();

        if ( ! itemsWithoutGroupId.isEmpty()) {
            result.addAll(itemsWithoutGroupId);
        }

        if ( ! itemsByGroupId.isEmpty()) {
            //We DON'T sort each group by creation date because
            //groupingBy preserve the order (if the downstream collector is in order, which
            //toList is)
            result.addAll(List.copyOf(itemsByGroupId.values()));
        }
        return result;
    }
}

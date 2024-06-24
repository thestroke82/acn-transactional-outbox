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
    public List<OutboxItem> execute(List<OutboxItem> outstandingItems) {
        //Found a test requiring this
        if (outstandingItems == null) {
            return null;
        }

        //Map from each group ID to a list of items
        //Stupid groupingBy collector requires non-null keys
        var itemsByGroupId = outstandingItems
                .stream()
                .collect(Collectors.groupingBy(item -> Optional.ofNullable(item.getGroupId())));

        //All the items with a null group ID
        var itemsWithoutGroupId = itemsByGroupId.getOrDefault(Optional.<String>empty(), List.of());

        //The first of each list (except the list associated with a null group ID)
        itemsByGroupId.remove(Optional.<String>empty());
        var firstsOfEachGroupId = itemsByGroupId
                .values()
                .stream()
                .map(items -> Collections.min(items, Comparator.comparing(OutboxItem::getCreationDate)))
                .toList();

        //The concatenation of the two lists
        var result = new ArrayList<OutboxItem>();
        result.addAll(itemsWithoutGroupId);
        result.addAll(firstsOfEachGroupId);
        return result;
    }
}

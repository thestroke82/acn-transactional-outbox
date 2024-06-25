package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.model.OutboxItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NullableIdGroupingStrategyTest {

    private NullableIdGroupingStrategy nullableIdGroupingStrategy;

    @BeforeEach
    public void setup() {
        nullableIdGroupingStrategy = new NullableIdGroupingStrategy();
    }

    @Test
    public void given_no_outstanding_items_when_execute_then_return_empty() {
        var result = nullableIdGroupingStrategy.group(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void given_only_null_group_id_items_when_execute_then_return_all() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId(null);
        outboxItem1.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId(null);
        outboxItem2.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));

        var result = nullableIdGroupingStrategy.group(List.of(outboxItem1, outboxItem2));
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(List.of(outboxItem1), List.of(outboxItem2))));
    }

    @Test
    public void given_single_non_null_group_id_with_1item_when_execute_then_return_it() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));

        var result = nullableIdGroupingStrategy.group(List.of(outboxItem1));
        assertEquals(1, result.size());
        assertEquals(List.of(outboxItem1), result.get(0));
    }


    @Test
    public void given_single_non_null_group_id_with_2items_when_execute_then_return_oldest() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));

        var result = nullableIdGroupingStrategy.group(List.of(outboxItem1, outboxItem2));
        assertEquals(1, result.size());
        assertEquals(List.of(outboxItem1, outboxItem2), result.get(0));
    }


    @Test
    public void given_2non_null_group_ids_with_2items_each_when_execute_then_return_oldests() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));

        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId("group2");
        outboxItem3.setCreationDate(Instant.now().minus(20, ChronoUnit.MINUTES));
        var outboxItem4 = new OutboxItem();
        outboxItem4.setGroupId("group2");
        outboxItem4.setCreationDate(Instant.now().minus(15, ChronoUnit.MINUTES));

        var result = nullableIdGroupingStrategy.group(
                List.of(outboxItem3, outboxItem4, outboxItem1, outboxItem2));

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());

        var exp1 = List.of(outboxItem1, outboxItem2);
        var exp2 = List.of(outboxItem3, outboxItem4);
        assertTrue(result.containsAll(List.of(exp1, exp2)));

    }

    @Test
    public void given_2non_null_group_ids_with_2items_each_and_the_null_group_when_execute_then_return_oldests_and_all_nulls() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));

        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId("group2");
        outboxItem3.setCreationDate(Instant.now().minus(20, ChronoUnit.MINUTES));
        var outboxItem4 = new OutboxItem();
        outboxItem4.setGroupId("group2");
        outboxItem4.setCreationDate(Instant.now().minus(15, ChronoUnit.MINUTES));

        var outboxItem5 = new OutboxItem();
        outboxItem5.setGroupId(null);
        outboxItem5.setCreationDate(Instant.now().minus(30, ChronoUnit.MINUTES));
        var outboxItem6 = new OutboxItem();
        outboxItem6.setGroupId(null);
        outboxItem6.setCreationDate(Instant.now().minus(25, ChronoUnit.MINUTES));

        var result = nullableIdGroupingStrategy.group(
                List.of(outboxItem5, outboxItem6, outboxItem3, outboxItem4, outboxItem1, outboxItem2));
        assertEquals(4, result.size());
        assertTrue(result.containsAll(
                List.of(
                        List.of(outboxItem1, outboxItem2),
                        List.of(outboxItem3, outboxItem4),
                        List.of(outboxItem5),
                        List.of(outboxItem6)
                )
        ));
    }

    @Test
    public void given_2items_with_same_date_when_execute_return_any() {
        var date = Instant.now().minus(10, ChronoUnit.MINUTES);
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(date);
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(date);


        var result = nullableIdGroupingStrategy.group(
                List.of(outboxItem2, outboxItem1));
        assertEquals(1, result.size());
        assertEquals(List.of(List.of(outboxItem2, outboxItem1)), result);
    }

    @Test
    public void given_2items_with_same_date_and_null_group_when_execute_return_any_and_all() {
        var date = Instant.now().minus(10, ChronoUnit.MINUTES);
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(date);
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(date);
        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId(null);
        outboxItem3.setCreationDate(date);
        var outboxItem4 = new OutboxItem();
        outboxItem4.setGroupId(null);
        outboxItem4.setCreationDate(date);

        var result = nullableIdGroupingStrategy.group(
                List.of(outboxItem2, outboxItem1, outboxItem4, outboxItem3));
        assertEquals(3, result.size());
        assertTrue(result.containsAll(List.of(
                List.of(outboxItem2, outboxItem1),
                List.of(outboxItem4),
                List.of(outboxItem3)
        )));
    }

    @Test
    public void given_null_list_when_execute_then_return_null() {
        assertNull(nullableIdGroupingStrategy.group(null));
    }

}
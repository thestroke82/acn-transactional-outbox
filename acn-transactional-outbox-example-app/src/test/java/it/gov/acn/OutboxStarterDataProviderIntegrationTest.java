package it.gov.acn;

import it.gov.acn.autoconfigure.outbox.providers.postgres.PostgresJdbcDataProvider;
import it.gov.acn.outboxprocessor.model.DataProvider;
import it.gov.acn.outboxprocessor.model.OutboxItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


// integration testing based on the sample data in db/migration/V1__outbox_table_and_sample_data.sql
@SpringBootTest(properties = {
        "acn.outbox.scheduler.enabled=true",
        "acn.outbox.scheduler.fixed-delay=3000",
})
public class OutboxStarterDataProviderIntegrationTest extends PostgresTestContext {

  @Autowired
  private DataProvider dataProvider;


  @Test
  void contextLoads() {
    assertTrue(dataProvider instanceof PostgresJdbcDataProvider);
  }

  @Test
  void when_find_some_results_are_returned() {
    List<OutboxItem> result = dataProvider.find(true, 1);
    assertNotNull(result);
    assertTrue(result.size()>0);
  }


  @Test
  void when_find_with_completed_true_returns_completed_items() {
    List<OutboxItem> result = dataProvider.find(true, 1);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi->oi.getCompletionDate()!=null));
  }

  @Test
  void when_find_with_completed_false_returns_incomplete_items() {
    List<OutboxItem> result = dataProvider.find(false, 2);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi->oi.getCompletionDate()==null));
  }

  @Test
  void when_find_with_max_attempts_filters_correctly() {
    List<OutboxItem> result = dataProvider.find(false, 2);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getAttempts() <= 2));

    result = dataProvider.find(false, 3);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getAttempts() <= 3));
  }

  @Test
  void when_find_with_different_completed_and_attempts_criteria() {
    List<OutboxItem> result = dataProvider.find(true, 3);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getCompletionDate() != null && oi.getAttempts() <= 3));

    result = dataProvider.find(false, 2);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getCompletionDate() == null && oi.getAttempts() <= 2));

  }

  @Test
  void when_find_with_edge_cases_fuckin_break_it() {
    // Test with the maximum allowed attempts
    List<OutboxItem> result = dataProvider.find(false, Integer.MAX_VALUE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getAttempts() <= Integer.MAX_VALUE));

    // Test with no attempts allowed (should return empty)
    result = dataProvider.find(false, 0);
    assertNotNull(result);
    assertTrue(result.isEmpty());

    // Test with completed items and maximum allowed attempts
    result = dataProvider.find(true, Integer.MAX_VALUE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getCompletionDate() != null && oi.getAttempts() <= Integer.MAX_VALUE));

    // Test with incomplete items and specific attempts
    result = dataProvider.find(false, 3);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getCompletionDate() == null && oi.getAttempts() <= 3));

    // Test with completed items and specific attempts
    result = dataProvider.find(true, 1);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(oi -> oi.getCompletionDate() != null && oi.getAttempts() <= 1));
  }

  @Test
    void when_save_then_outbox_item_is_persisted() {
        OutboxItem newItem = new OutboxItem();
        UUID newId = UUID.randomUUID();
        newItem.setId(newId);
        newItem.setEventType("NEW_EVENT_TYPE");
        newItem.setCreationDate(Instant.now());
        newItem.setLastAttemptDate(Instant.now());
        newItem.setCompletionDate(Instant.now());
        newItem.setAttempts(0);
        newItem.setEvent("NEW_EVENT_DATA");
        newItem.setLastError(null);

        dataProvider.save(newItem);

        OutboxItem result = dataProvider.findById(newId);
        assertNotNull(result);
        assertEquals(newItem.getId(), result.getId());
        assertEquals(newItem.getEventType(), result.getEventType());
        assertEquals(newItem.getCreationDate().toEpochMilli(), result.getCreationDate().toEpochMilli());
        assertEquals(newItem.getLastAttemptDate().toEpochMilli(), result.getLastAttemptDate().toEpochMilli());
        assertEquals(newItem.getCompletionDate().toEpochMilli(), result.getCompletionDate().toEpochMilli());
        assertEquals(newItem.getAttempts(), result.getAttempts());
        assertEquals(newItem.getEvent(), result.getEvent());
        assertEquals(newItem.getLastError(), result.getLastError());
    }

    @Test
    void when_update_then_outbox_item_is_updated() {
        // First, save a new item
        OutboxItem newItem = new OutboxItem();
        UUID newId = UUID.randomUUID();
        newItem.setId(newId);
        newItem.setEventType("EVENT_TYPE_TO_UPDATE");
        newItem.setCreationDate(Instant.now());
        newItem.setLastAttemptDate(Instant.now());
        newItem.setCompletionDate(Instant.now());
        newItem.setAttempts(0);
        newItem.setEvent("EVENT_DATA_TO_UPDATE");
        newItem.setLastError(null);

        dataProvider.save(newItem);

        // Update the saved item
        newItem.setEventType("UPDATED_EVENT_TYPE");
        newItem.setAttempts(1);
        newItem.setEvent("UPDATED_EVENT_DATA");
        newItem.setLastError("UPDATED_ERROR");

        dataProvider.update(newItem);

        OutboxItem result = dataProvider.findById(newId);
        assertNotNull(result);
        assertEquals("UPDATED_EVENT_TYPE", result.getEventType());
        assertEquals(1, result.getAttempts());
        assertEquals("UPDATED_EVENT_DATA", result.getEvent());
        assertEquals("UPDATED_ERROR", result.getLastError());
    }
}

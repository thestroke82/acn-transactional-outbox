package it.gov.acn;

import it.gov.acn.outbox.core.recorder.OutboxEventRecorder;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.TransactionManagerProvider;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=30000",
})
public class TransactionManagerProviderTest extends PostgresTestContainerConfiguration{

  @Autowired
  private DataProvider dataProvider;
  @Autowired
  private TransactionManagerProvider transactionManagerProvider;
  @Autowired
  private OutboxEventRecorder outboxEventRecorder;

  @Transactional
  @Test
  public void given_outer_transactional_context_when_exception_bottom_then_nothing_is_saved(){
    Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    Assertions.assertThrows(RuntimeException.class, () -> {
      this.outboxEventRecorder.recordEvent("{\"test\":\"test\"}", "test");
      throw new RuntimeException("test");
    });
    List<OutboxItem> allItems = this.dataProvider.find(false,Integer.MAX_VALUE);
    Assertions.assertEquals(0, allItems.size());

    this.outboxEventRecorder.recordEvent("{\"test\":\"test\"}", "test");
    allItems = this.dataProvider.find(false,Integer.MAX_VALUE);
    Assertions.assertEquals(1, allItems.size());
  }
}

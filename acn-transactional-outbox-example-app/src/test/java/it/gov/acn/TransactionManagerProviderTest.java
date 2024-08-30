package it.gov.acn;

import it.gov.acn.etc.TransactionTestClass;
import it.gov.acn.outbox.core.recorder.OutboxEventRecorder;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.TransactionManagerProvider;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=30000",
})
@Import(TransactionTestClass.class)
public class TransactionManagerProviderTest extends PostgresTestContainerConfiguration {

  @Autowired
  private DataProvider dataProvider;
  @Autowired
  private TransactionManagerProvider transactionManagerProvider;
  @Autowired
  private OutboxEventRecorder outboxEventRecorder;
  @Autowired
  private TransactionTestClass transactionTestClass;

  @Test
  public void given_outer_transactional_context_when_exception_bottom_then_nothing_is_saved() {

    List<OutboxItem> allItems = this.dataProvider.find(false, Integer.MAX_VALUE);
    Assertions.assertEquals(0, allItems.size());

    Assertions.assertThrows(RuntimeException.class, () -> {
      this.transactionTestClass.throwExceptionAfterOutboxEventRecord();
    });

    allItems = this.dataProvider.find(false, Integer.MAX_VALUE);
    Assertions.assertEquals(0, allItems.size());

    this.outboxEventRecorder.recordEvent("{\"test\":\"test\"}", "test");
    allItems = this.dataProvider.find(false, Integer.MAX_VALUE);
    Assertions.assertEquals(1, allItems.size());
  }
}

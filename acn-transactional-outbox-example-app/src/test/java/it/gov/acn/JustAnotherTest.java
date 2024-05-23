package it.gov.acn;

import it.gov.acn.config.ErrorMessagesHolder.ErrorReporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=3000",
})
public class JustAnotherTest extends PostgresTestContext {

  @Autowired(required = false)
  private ErrorReporter errorReporter;


  @Test
  void test_temporary_beans_are_garbage_collected() throws InterruptedException {
    System.out.println("Just another test");
    Thread.sleep(10000);
    Assertions.assertNull(errorReporter);
  }

}

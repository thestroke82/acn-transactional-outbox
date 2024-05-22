package it.gov.acn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=1500",
})
public class JustAnotherTest extends PostgresTestContext {

  @Test
  void test() throws InterruptedException {
    System.out.println("Just another test");
    Thread.sleep(10000);
  }

}

package it.gov.acn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=3000",
})
public class JustAnotherTest extends PostgresTestContext {


  @Test
  void test() throws InterruptedException {
    System.out.println("Just another test");
    Thread.sleep(10000);
  }

}

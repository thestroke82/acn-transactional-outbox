package it.gov.acn.config;

import it.gov.acn.ContextRunnerDecorator;
import it.gov.acn.TestConfiguration;
import it.gov.acn.TransactionalOutboxScheduler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class OutboxStarterAutoconfigurationTests {

  private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(TransactionalOutboxAutoconfiguration.class));



  @Test
  void should_provide_ThreadPoolTaskScheduler_when_no_other_TaskScheduler_present() {
    ContextRunnerDecorator.create(contextRunner)
        .withEnabled(true)
        .withFixedDelay(3000)
        .withDatasource(true)
        .withTransactionManager()
        .claim()
        .run(context -> {
          Assertions.assertThat(context).hasSingleBean(TaskScheduler.class);
          Assertions.assertThat(context).hasBean("threadPoolTaskScheduler");
          Assertions.assertThat(context.getBean("threadPoolTaskScheduler")).isInstanceOf(ThreadPoolTaskScheduler.class);
        });
  }

  @Test
  void should_not_provide_ThreadPoolTaskScheduler_when_other_TaskScheduler_present() {
    ContextRunnerDecorator.create(contextRunner)
        .withUserConfiguration(TestConfiguration.class)
        .withEnabled(true)
        .withDatasource(true)
        .withTransactionManager()
        .claim()
        .run(context -> {
          Assertions.assertThat(context).hasSingleBean(TaskScheduler.class);
          Assertions.assertThat(context).doesNotHaveBean("threadPoolTaskScheduler");
        });
  }

  @Test
  void should_provide_transactionalOutboxScheduler() {
    ContextRunnerDecorator.create(contextRunner)
        .withEnabled(true)
        .withDatasource(true)
        .withTransactionManager()
        .claim()
        .run(context -> {
          Assertions.assertThat(context).hasSingleBean(TransactionalOutboxScheduler.class);
          Assertions.assertThat(context).hasBean("transactionalOutboxScheduler");
        });
  }

  @Test
  void should_not_provide_transactionalOutboxScheduler_when_properties_not_configured() {
    ContextRunnerDecorator.create(contextRunner)
        .claim()
        .run(context -> {
          Assertions.assertThat(context).doesNotHaveBean(TransactionalOutboxScheduler.class);
          Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
        });
  }

  @Test
  void should_not_provide_transactionalOutboxScheduler_when_context_fixedDelay_invalid() {
    ContextRunnerDecorator.create(contextRunner)
        .withEnabled(true)
        .withFixedDelay(-1)
        .withDatasource(true)
        .withTransactionManager()
        .claim()
        .run(context -> {
          Assertions.assertThat(context).doesNotHaveBean(TransactionalOutboxScheduler.class);
          Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
        });
  }

  @Test
  void should_not_provide_transactionalOutboxScheduler_when_datasource_is_not_present() {
    ContextRunnerDecorator.create(contextRunner)
        .withEnabled(true)
        .withTransactionManager()
        .withFixedDelay(3000)
        .claim()
        .run(context -> {
          Assertions.assertThat(context).doesNotHaveBean(TransactionalOutboxScheduler.class);
          Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
        });
  }

//    @Test
//    void should_not_provide_transactionalOutboxScheduler_when_datasource_is_present_but_not_postgres() {
//        it.gov.acn.ContextRunnerDecorator.create(contextRunner)
//                .withEnabled(true)
//                .withFixedDelay(3000)
//                .withDatasource(false)
//                .claim()
//                .run(context -> {
//                    assertThat(context).doesNotHaveBean(TransactionalOutboxScheduler.class);
//                    assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
//                });
//    }

  @Test
  void should_not_provide_transactionalOutboxScheduler_when_scheduler_not_enabled() {
    ContextRunnerDecorator.create(contextRunner)
        .withEnabled(false)
        .withDatasource(true)
        .withTransactionManager()
        .claim()
        .run(context -> {
          Assertions.assertThat(context).doesNotHaveBean(TransactionalOutboxScheduler.class);
          Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
        });
  }

}

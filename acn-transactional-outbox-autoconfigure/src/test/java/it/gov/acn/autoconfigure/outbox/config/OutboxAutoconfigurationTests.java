package it.gov.acn.autoconfigure.outbox.config;

import it.gov.acn.autoconfigure.outbox.ContextRunnerDecorator;
import it.gov.acn.autoconfigure.outbox.OutboxScheduler;
import it.gov.acn.autoconfigure.outbox.condition.requirement.ContextRequirementsValidator;
import it.gov.acn.autoconfigure.outbox.etc.Utils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

public class OutboxAutoconfigurationTests {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BulkheadAutoConfiguration.class, OutboxAutoconfiguration.class));
    private static MockedStatic<Utils> mockedStatic;

    @BeforeAll
    static void setUp() {
        // Initialize the MockedStatic instance
        mockedStatic = Mockito.mockStatic(Utils.class);
        // Define the behavior of the static method
        mockedStatic
                .when(()-> Utils.doesTableExist(Mockito.any(), Mockito.any()))
                .thenReturn(true);
        // leave the rest of the methods to their default behavior
        mockedStatic.when(()->Utils.isPostgresDatasource(Mockito.any())).thenCallRealMethod();
        mockedStatic.when(()->Utils.isBeanPresentInContext(Mockito.any(),Mockito.any())).thenCallRealMethod();
    }

    @AfterAll
    static void tearDown() {
        // Close the MockedStatic instance
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(ContextRequirementsValidator.class, "instance", null);
    }

    private final Class<?>[] autoConfigurations =
            {BulkheadAutoConfiguration.class, OutboxAutoconfiguration.class};

    @Test
    void should_provide_ThreadPoolTaskScheduler_when_no_other_TaskScheduler_present() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(true)
                .withDatasource()
                .withTransactionManager()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).hasSingleBean(TaskScheduler.class);
                    Assertions.assertThat(context).hasBean("threadPoolTaskScheduler");
                    Assertions.assertThat(context.getBean("threadPoolTaskScheduler"))
                            .isInstanceOf(ThreadPoolTaskScheduler.class);
                });
    }

    @Test
    void should_not_provide_ThreadPoolTaskScheduler_when_other_TaskScheduler_present() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(true)
                .withDatasource()
                .withTransactionManager()
                .withTaskScheduler()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).hasSingleBean(TaskScheduler.class);
                    Assertions.assertThat(context).hasBean("testTaskScheduler");
                    Assertions.assertThat(context.getBean("testTaskScheduler"))
                            .isInstanceOf(ThreadPoolTaskScheduler.class);
                });
    }

    @Test
    void should_provide_transactionalOutboxScheduler() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(true)
                .withDatasource()
                .withTransactionManager()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).hasSingleBean(OutboxScheduler.class);
                    Assertions.assertThat(context).hasBean("transactionalOutboxScheduler");
                });
    }

    @Test
    void should_not_provide_transactionalOutboxScheduler_when_properties_not_configured() {
        ContextRunnerDecorator.create(contextRunner)
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).doesNotHaveBean(OutboxScheduler.class);
                    Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
                });
    }

    @Test
    void should_not_provide_transactionalOutboxScheduler_when_context_fixedDelay_invalid() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(true)
                .withFixedDelay(-1)
                .withDatasource()
                .withTransactionManager()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).doesNotHaveBean(OutboxScheduler.class);
                    Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
                });
    }

    @Test
    void should_not_provide_transactionalOutboxScheduler_when_datasource_is_not_present() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(true)
                .withTransactionManager()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).doesNotHaveBean(OutboxScheduler.class);
                    Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
                });
    }

//    @Test
//    void should_not_provide_transactionalOutboxScheduler_when_datasource_is_present_but_not_the_db_table() {
//        ContextRunnerDecorator.create(contextRunner)
//                .withEnabled(true)
//                .withDatasource()
//                .withTransactionManager()
//                .claim()
//                .run(context -> {
//                    Assertions.assertThat(context).doesNotHaveBean(OutboxScheduler.class);
//                    Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
//                });
//    }

    @Test
    void should_not_provide_transactionalOutboxScheduler_when_scheduler_not_enabled() {
        ContextRunnerDecorator.create(contextRunner)
                .withEnabled(false)
                .withDatasource()
                .withTransactionManager()
                .claim()
                .run(context -> {
                    Assertions.assertThat(context).doesNotHaveBean(OutboxScheduler.class);
                    Assertions.assertThat(context).doesNotHaveBean("transactionalOutboxScheduler");
                });
    }

}

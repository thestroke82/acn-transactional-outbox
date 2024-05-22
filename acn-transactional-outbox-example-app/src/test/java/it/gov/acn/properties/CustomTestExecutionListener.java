package it.gov.acn.properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.TestPropertySourceUtils;

public class CustomTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext){
        TestProperties testProperties = testContext.getTestMethod().getAnnotation(TestProperties.class);
        if (testProperties != null) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                (ConfigurableApplicationContext) testContext.getApplicationContext(),
                testProperties.value()
            );
        }
    }
}
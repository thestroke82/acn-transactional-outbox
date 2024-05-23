package it.gov.acn.outboxprocessor;

import it.gov.acn.outboxprocessor.model.DataProvider;

public class OutboxProcessorConfiguration {
    private String testString;

    private DataProvider dataProvider;

    public OutboxProcessorConfiguration(String message, DataProvider dataProvider) {
        this.testString = message;
    }

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
}

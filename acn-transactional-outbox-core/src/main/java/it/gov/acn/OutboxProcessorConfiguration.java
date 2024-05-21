package it.gov.acn;

public class OutboxProcessorConfiguration {

    private String testPhrase;

    public OutboxProcessorConfiguration(String testPhase) {
        this.testPhrase = testPhase;
    }

    public OutboxProcessorConfiguration() {
    }

    public String getTestPhrase() {
        return testPhrase;
    }

    public void setTestPhrase(String testPhrase) {
        this.testPhrase = testPhrase;
    }
}

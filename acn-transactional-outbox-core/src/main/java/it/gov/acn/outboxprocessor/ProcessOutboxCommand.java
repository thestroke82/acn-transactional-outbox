package it.gov.acn.outboxprocessor;

public class ProcessOutboxCommand {
    private String message;

    public ProcessOutboxCommand(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package it.gov.acn.config;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessagesHolder {
    private final static List<String> errorMessages = new ArrayList<>();

    public static void addErrorMessage(String errorMessage) {
        if(!errorMessages.contains(errorMessage)) {
            errorMessages.add(errorMessage);
        }
    }
    public static List<String> getErrorMessages() {
        return errorMessages;
    }

    public static class ErrorReporter {
        public ErrorReporter(String errorMessage) {
            ErrorMessagesHolder.addErrorMessage(errorMessage);
        }
    }
}

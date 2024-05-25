package it.gov.acn.autoconfigure.outbox.providers;

public enum OutboxSqlColumns {
    ID("id"),
    EVENT_TYPE("event_type"),
    CREATION_DATE("creation_date"),
    LAST_ATTEMPT_DATE("last_attempt_date"),
    COMPLETION_DATE("completion_date"),
    ATTEMPTS("attempts"),
    EVENT("event"),
    LAST_ERROR("last_error");

    private final String columnName;

    OutboxSqlColumns(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public static String getAllColumns() {
        return String.join(", ",
                ID.getColumnName(),
                EVENT_TYPE.getColumnName(),
                CREATION_DATE.getColumnName(),
                LAST_ATTEMPT_DATE.getColumnName(),
                COMPLETION_DATE.getColumnName(),
                ATTEMPTS.getColumnName(),
                EVENT.getColumnName(),
                LAST_ERROR.getColumnName());
    }
}

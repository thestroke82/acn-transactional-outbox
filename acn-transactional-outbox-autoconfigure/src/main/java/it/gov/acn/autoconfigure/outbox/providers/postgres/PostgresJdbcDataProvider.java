package it.gov.acn.autoconfigure.outbox.providers.postgres;

import it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns;
import it.gov.acn.outboxprocessor.model.DataProvider;
import it.gov.acn.outboxprocessor.model.OutboxItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.ATTEMPTS;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.COMPLETION_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.CREATION_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.EVENT;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.EVENT_TYPE;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.ID;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.LAST_ATTEMPT_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.OutboxSqlColumns.LAST_ERROR;

public class PostgresJdbcDataProvider implements DataProvider {

    private final DataSource dataSource;

    public PostgresJdbcDataProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<OutboxItem> find(boolean completed, int maxAttempts) {
        List<OutboxItem> outboxItems = new ArrayList<>();
        String sql = "SELECT " + OutboxSqlColumns.getAllColumns() +
                " FROM transactional_outbox WHERE (completion_date IS " + (completed ? "NOT " : "") + "NULL) AND attempts <= ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, maxAttempts);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    OutboxItem item = mapResultSetToOutboxItem(resultSet);
                    outboxItems.add(item);
                }
            }
        } catch (SQLException e) {
            // Log and handle the exception appropriately
            e.printStackTrace();
        }

        return outboxItems;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private OutboxItem mapResultSetToOutboxItem(ResultSet resultSet) throws SQLException {
        OutboxItem item = new OutboxItem();
        item.setId(resultSet.getObject(ID.getColumnName(), UUID.class));
        item.setEventType(resultSet.getString(EVENT_TYPE.getColumnName()));
        item.setCreationDate(toInstant(resultSet.getTimestamp(CREATION_DATE.getColumnName())));
        item.setLastAttemptDate(toInstant(resultSet.getTimestamp(LAST_ATTEMPT_DATE.getColumnName())));
        item.setCompletionDate(toInstant(resultSet.getTimestamp(COMPLETION_DATE.getColumnName())));
        item.setAttempts(resultSet.getInt(ATTEMPTS.getColumnName()));
        item.setEvent(resultSet.getString(EVENT.getColumnName()));
        item.setLastError(resultSet.getString(LAST_ERROR.getColumnName()));
        return item;
    }
}

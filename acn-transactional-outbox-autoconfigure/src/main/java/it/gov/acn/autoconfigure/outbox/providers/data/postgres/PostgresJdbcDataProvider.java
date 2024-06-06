package it.gov.acn.autoconfigure.outbox.providers.data.postgres;

import it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.Sort;

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

import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.ATTEMPTS;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.COMPLETION_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.CREATION_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.EVENT;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.EVENT_TYPE;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.ID;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.LAST_ATTEMPT_DATE;
import static it.gov.acn.autoconfigure.outbox.providers.data.OutboxSqlColumns.LAST_ERROR;

public class PostgresJdbcDataProvider implements DataProvider {

    private final DataSource dataSource;

    public PostgresJdbcDataProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<OutboxItem> find(boolean completed, int maxAttempts) {
        return find(completed, maxAttempts, Sort.defaultSort());
    }
    @Override
    public List<OutboxItem> find(boolean completed, int maxAttempts, Sort sort) {
        List<OutboxItem> outboxItems = new ArrayList<>();

        String sql = "SELECT " + OutboxSqlColumns.getAllColumns() +
            " FROM transactional_outbox WHERE (completion_date IS " + (completed ? "NOT " : "") + "NULL) AND attempts <= ?" +
            " ORDER BY " + getColumnName(sort.getProperty()) + " " + getDirection(sort.getDirection());

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
            throw new RuntimeException("Error while finding outbox items", e);
        }

        return outboxItems;
    }

    @Override
    public OutboxItem findById(UUID id) {
        OutboxItem outboxItem;
        String sql = "SELECT " + OutboxSqlColumns.getAllColumns() +
                " FROM transactional_outbox WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                outboxItem = resultSet.next() ? mapResultSetToOutboxItem(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding outbox item by id", e);
        }

        return outboxItem;
    }

    @Override
    public void save(OutboxItem item) {
        String sql = "INSERT INTO transactional_outbox (" + String.join(", ",
                ID.getColumnName(),
                EVENT_TYPE.getColumnName(),
                CREATION_DATE.getColumnName(),
                LAST_ATTEMPT_DATE.getColumnName(),
                COMPLETION_DATE.getColumnName(),
                ATTEMPTS.getColumnName(),
                EVENT.getColumnName(),
                LAST_ERROR.getColumnName()) +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, item.getId());
            preparedStatement.setString(2, item.getEventType());
            preparedStatement.setTimestamp(3, Timestamp.from(item.getCreationDate()));
            preparedStatement.setTimestamp(4, item.getLastAttemptDate() != null ? Timestamp.from(item.getLastAttemptDate()) : null);
            preparedStatement.setTimestamp(5, item.getCompletionDate() != null ? Timestamp.from(item.getCompletionDate()) : null);
            preparedStatement.setInt(6, item.getAttempts());
            preparedStatement.setString(7, item.getEvent());
            preparedStatement.setString(8, item.getLastError());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while saving outbox item", e);
        }
    }

    @Override
    public void update(OutboxItem item) {
        String sql = "UPDATE transactional_outbox SET " +
                EVENT_TYPE.getColumnName() + " = ?, " +
                CREATION_DATE.getColumnName() + " = ?, " +
                LAST_ATTEMPT_DATE.getColumnName() + " = ?, " +
                COMPLETION_DATE.getColumnName() + " = ?, " +
                ATTEMPTS.getColumnName() + " = ?, " +
                EVENT.getColumnName() + " = ?, " +
                LAST_ERROR.getColumnName() + " = ? " +
                "WHERE " + ID.getColumnName() + " = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, item.getEventType());
            preparedStatement.setTimestamp(2, Timestamp.from(item.getCreationDate()));
            preparedStatement.setTimestamp(3, item.getLastAttemptDate() != null ? Timestamp.from(item.getLastAttemptDate()) : null);
            preparedStatement.setTimestamp(4, item.getCompletionDate() != null ? Timestamp.from(item.getCompletionDate()) : null);
            preparedStatement.setInt(5, item.getAttempts());
            preparedStatement.setString(6, item.getEvent());
            preparedStatement.setString(7, item.getLastError());
            preparedStatement.setObject(8, item.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while updating outbox item", e);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private String getColumnName(Sort.Property property) {
      switch (property) {
        case CREATION_DATE -> {
          return OutboxSqlColumns.CREATION_DATE.getColumnName();
        }
        case LAST_ATTEMPT_DATE -> {
          return OutboxSqlColumns.LAST_ATTEMPT_DATE.getColumnName();
        }
        case COMPLETION_DATE -> {
          return OutboxSqlColumns.COMPLETION_DATE.getColumnName();
        }
        case ATTEMPTS -> {
          return OutboxSqlColumns.ATTEMPTS.getColumnName();
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + property);
      }
    }
    private String getDirection(Sort.Direction direction) {
      switch (direction) {
        case ASC -> {
          return "ASC";
        }
        case DESC -> {
          return "DESC";
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + direction);
      }
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

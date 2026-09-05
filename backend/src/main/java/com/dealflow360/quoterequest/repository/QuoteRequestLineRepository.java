package com.dealflow360.quoterequest.repository;

import com.dealflow360.quoterequest.model.QuoteRequestLine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class QuoteRequestLineRepository {

    private static final String INSERT =
            """
            INSERT INTO quote_request_lines (
              quote_request_id, product_id, quantity, notes, expected_discount_percent
            ) VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, quote_request_id, product_id, quantity, notes, expected_discount_percent,
                   created_at, updated_at
            FROM quote_request_lines
            """;
    private static final String UPDATE =
            """
            UPDATE quote_request_lines
            SET quantity = ?, notes = ?, expected_discount_percent = ?
            WHERE id = ? AND quote_request_id = ?
            """;
    private static final String DELETE = "DELETE FROM quote_request_lines WHERE id = ? AND quote_request_id = ?";

    private final DataSource dataSource;

    public QuoteRequestLineRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public QuoteRequestLine insert(
            long quoteRequestId,
            long productId,
            BigDecimal quantity,
            String notes,
            BigDecimal expectedDiscountPercent) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, quoteRequestId);
            statement.setLong(2, productId);
            statement.setBigDecimal(3, quantity);
            statement.setString(4, notes == null ? "" : notes);
            if (expectedDiscountPercent == null) {
                statement.setNull(5, Types.DECIMAL);
            } else {
                statement.setBigDecimal(5, expectedDiscountPercent);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert request line returned no id");
                }
                return findById(keys.getLong(1), quoteRequestId)
                        .orElseThrow(() -> new SQLException("Inserted request line not found"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<QuoteRequestLine> findById(long id, long quoteRequestId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ? AND quote_request_id = ?")) {
            statement.setLong(1, id);
            statement.setLong(2, quoteRequestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<QuoteRequestLine> findByRequest(long quoteRequestId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(SELECT + " WHERE quote_request_id = ? ORDER BY id")) {
            statement.setLong(1, quoteRequestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuoteRequestLine> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(map(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public boolean update(
            long id,
            long quoteRequestId,
            BigDecimal quantity,
            String notes,
            BigDecimal expectedDiscountPercent) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setBigDecimal(1, quantity);
            statement.setString(2, notes == null ? "" : notes);
            if (expectedDiscountPercent == null) {
                statement.setNull(3, Types.DECIMAL);
            } else {
                statement.setBigDecimal(3, expectedDiscountPercent);
            }
            statement.setLong(4, id);
            statement.setLong(5, quoteRequestId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public boolean delete(long id, long quoteRequestId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setLong(1, id);
            statement.setLong(2, quoteRequestId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static QuoteRequestLine map(ResultSet resultSet) throws SQLException {
        return new QuoteRequestLine(
                resultSet.getLong("id"),
                resultSet.getLong("quote_request_id"),
                resultSet.getLong("product_id"),
                resultSet.getBigDecimal("quantity"),
                resultSet.getString("notes"),
                resultSet.getBigDecimal("expected_discount_percent"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

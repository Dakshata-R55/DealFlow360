package com.dealflow360.quoterequest.repository;

import com.dealflow360.quoterequest.model.QuoteRequest;
import com.dealflow360.quoterequest.model.QuoteRequestStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class QuoteRequestRepository {

    private static final String INSERT =
            """
            INSERT INTO quote_requests (
              request_number, customer_user_id, seller_company_id, status,
              requested_delivery_date, target_budget, expected_discount_percent, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, request_number, customer_user_id, seller_company_id, status,
                   requested_delivery_date, target_budget, expected_discount_percent, notes, quotation_id,
                   created_at, updated_at, submitted_at
            FROM quote_requests
            """;
    private static final String BUMP_SEQUENCE =
            """
            INSERT INTO quote_request_number_sequences (year, last_number)
            VALUES (?, 1)
            ON DUPLICATE KEY UPDATE last_number = last_number + 1
            """;
    private static final String READ_SEQUENCE =
            "SELECT last_number FROM quote_request_number_sequences WHERE year = ?";
    private static final String PATCH =
            """
            UPDATE quote_requests
            SET requested_delivery_date = ?, target_budget = ?, expected_discount_percent = ?, notes = ?
            WHERE id = ? AND customer_user_id = ? AND status = 'DRAFT'
            """;
    private static final String UPDATE_STATUS =
            """
            UPDATE quote_requests
            SET status = ?, submitted_at = ?, quotation_id = ?
            WHERE id = ?
            """;

    private final DataSource dataSource;

    public QuoteRequestRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String nextRequestNumber() {
        int year = Year.now().getValue();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement bump = connection.prepareStatement(BUMP_SEQUENCE);
                PreparedStatement read = connection.prepareStatement(READ_SEQUENCE)) {
            bump.setInt(1, year);
            bump.executeUpdate();
            read.setInt(1, year);
            try (ResultSet resultSet = read.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Request number sequence missing after bump");
                }
                return "REQUEST-%d-%04d".formatted(year, resultSet.getInt("last_number"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public QuoteRequest insert(long customerUserId, long sellerCompanyId, String requestNumber) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, requestNumber);
            statement.setLong(2, customerUserId);
            statement.setLong(3, sellerCompanyId);
            statement.setString(4, QuoteRequestStatus.DRAFT.name());
            statement.setNull(5, Types.DATE);
            statement.setNull(6, Types.DECIMAL);
            statement.setNull(7, Types.DECIMAL);
            statement.setString(8, "");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert quote request returned no id");
                }
                return findById(keys.getLong(1)).orElseThrow(() -> new SQLException("Inserted quote request not found"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<QuoteRequest> findById(long id) {
        return queryOne(SELECT + " WHERE id = ?", statement -> statement.setLong(1, id));
    }

    public Optional<QuoteRequest> findByQuotationId(long quotationId) {
        return queryOne(SELECT + " WHERE quotation_id = ?", statement -> statement.setLong(1, quotationId));
    }

    public Optional<QuoteRequest> findDraft(long customerUserId, long sellerCompanyId) {
        return queryOne(
                SELECT + " WHERE customer_user_id = ? AND seller_company_id = ? AND status = 'DRAFT'",
                statement -> {
                    statement.setLong(1, customerUserId);
                    statement.setLong(2, sellerCompanyId);
                });
    }

    public List<QuoteRequest> findByCustomer(long customerUserId) {
        return queryList(SELECT + " WHERE customer_user_id = ? ORDER BY updated_at DESC, id DESC", statement -> {
            statement.setLong(1, customerUserId);
        });
    }

    public List<QuoteRequest> findBySeller(long sellerCompanyId) {
        return queryList(
                SELECT + " WHERE seller_company_id = ? AND status <> 'DRAFT' ORDER BY updated_at DESC, id DESC",
                statement -> statement.setLong(1, sellerCompanyId));
    }

    public boolean patchDraft(
            long id,
            long customerUserId,
            LocalDate requestedDeliveryDate,
            BigDecimal targetBudget,
            BigDecimal expectedDiscountPercent,
            String notes) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(PATCH)) {
            if (requestedDeliveryDate == null) {
                statement.setNull(1, Types.DATE);
            } else {
                statement.setDate(1, Date.valueOf(requestedDeliveryDate));
            }
            if (targetBudget == null) {
                statement.setNull(2, Types.DECIMAL);
            } else {
                statement.setBigDecimal(2, targetBudget);
            }
            if (expectedDiscountPercent == null) {
                statement.setNull(3, Types.DECIMAL);
            } else {
                statement.setBigDecimal(3, expectedDiscountPercent);
            }
            statement.setString(4, notes == null ? "" : notes);
            statement.setLong(5, id);
            statement.setLong(6, customerUserId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void updateStatus(long id, QuoteRequestStatus status, Instant submittedAt, Long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            if (submittedAt == null) {
                statement.setNull(2, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(2, Timestamp.from(submittedAt));
            }
            if (quotationId == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, quotationId);
            }
            statement.setLong(4, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private Optional<QuoteRequest> queryOne(String sql, SqlBinder binder) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
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

    private List<QuoteRequest> queryList(String sql, SqlBinder binder) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuoteRequest> rows = new ArrayList<>();
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

    private static QuoteRequest map(ResultSet resultSet) throws SQLException {
        Date delivery = resultSet.getDate("requested_delivery_date");
        Timestamp submitted = resultSet.getTimestamp("submitted_at");
        long quotationRaw = resultSet.getLong("quotation_id");
        Long quotationId = resultSet.wasNull() ? null : quotationRaw;
        return new QuoteRequest(
                resultSet.getLong("id"),
                resultSet.getString("request_number"),
                resultSet.getLong("customer_user_id"),
                resultSet.getLong("seller_company_id"),
                QuoteRequestStatus.valueOf(resultSet.getString("status")),
                delivery == null ? null : delivery.toLocalDate(),
                resultSet.getBigDecimal("target_budget"),
                resultSet.getBigDecimal("expected_discount_percent"),
                resultSet.getString("notes"),
                quotationId,
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                submitted == null ? null : submitted.toInstant());
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}

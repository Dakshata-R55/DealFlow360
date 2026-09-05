package com.dealflow360.quotation.repository;

import com.dealflow360.policy.model.RiskLevel;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationStatus;
import com.dealflow360.shared.exception.ConflictException;
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
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class QuotationRepository {

    private static final String INSERT =
            """
            INSERT INTO quotations (
              company_id, quote_number, customer_id, sales_rep_id, price_list_id, status,
              subtotal, discount_amount, total_amount, total_cost, margin_amount, margin_percent,
              risk_score, risk_level
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, quote_number, customer_id, sales_rep_id, price_list_id, status,
                   subtotal, discount_amount, total_amount, total_cost, margin_amount, margin_percent,
                   risk_score, risk_level, created_at, updated_at, submitted_at
            FROM quotations
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY updated_at DESC, id DESC";
    private static final String UPDATE_COMPUTED =
            """
            UPDATE quotations
            SET subtotal = ?, discount_amount = ?, total_amount = ?, total_cost = ?, margin_amount = ?,
                margin_percent = ?, risk_score = ?, risk_level = ?
            WHERE id = ? AND company_id = ?
            """;
    private static final String UPDATE_STATUS =
            """
            UPDATE quotations
            SET status = ?, submitted_at = ?, risk_score = ?, risk_level = ?
            WHERE id = ? AND company_id = ?
            """;
    private static final String ASSIGN_QUOTE_NUMBER =
            "UPDATE quotations SET quote_number = CONCAT('Q-', id) WHERE id = ? AND company_id = ?";
    private static final String INSERT_DISMISSAL =
            """
            INSERT INTO quotation_dismissed_suggestions (quotation_id, product_id)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE product_id = VALUES(product_id)
            """;
    private static final String FIND_DISMISSALS =
            "SELECT product_id FROM quotation_dismissed_suggestions WHERE quotation_id = ?";

    private final DataSource dataSource;

    public QuotationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Quotation insert(
            long companyId, long customerId, long salesRepId, long priceListId, QuotationStatus status) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement assignNumber = connection.prepareStatement(ASSIGN_QUOTE_NUMBER)) {
            statement.setLong(1, companyId);
            statement.setString(2, "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 31));
            statement.setLong(3, customerId);
            statement.setLong(4, salesRepId);
            statement.setLong(5, priceListId);
            statement.setString(6, status.name());
            statement.setBigDecimal(7, BigDecimal.ZERO);
            statement.setBigDecimal(8, BigDecimal.ZERO);
            statement.setBigDecimal(9, BigDecimal.ZERO);
            statement.setBigDecimal(10, BigDecimal.ZERO);
            statement.setBigDecimal(11, BigDecimal.ZERO);
            statement.setBigDecimal(12, BigDecimal.ZERO);
            statement.setBigDecimal(13, BigDecimal.ZERO);
            statement.setString(14, RiskLevel.NONE.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert quotation returned no id");
                }
                long id = keys.getLong(1);
                assignNumber.setLong(1, id);
                assignNumber.setLong(2, companyId);
                assignNumber.executeUpdate();
                return findById(id, companyId).orElseThrow(() -> new SQLException("Inserted quotation not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Quotation number already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Quotation> findById(long id, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, id);
            statement.setLong(2, companyId);
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

    public List<Quotation> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Quotation> rows = new ArrayList<>();
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

    public Optional<Quotation> updateComputed(
            long id,
            long companyId,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal totalCost,
            BigDecimal marginAmount,
            BigDecimal marginPercent,
            BigDecimal riskScore,
            RiskLevel riskLevel) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_COMPUTED)) {
            statement.setBigDecimal(1, subtotal);
            statement.setBigDecimal(2, discountAmount);
            statement.setBigDecimal(3, totalAmount);
            statement.setBigDecimal(4, totalCost);
            statement.setBigDecimal(5, marginAmount);
            statement.setBigDecimal(6, marginPercent);
            statement.setBigDecimal(7, riskScore);
            statement.setString(8, riskLevel.name());
            statement.setLong(9, id);
            statement.setLong(10, companyId);
            statement.executeUpdate();
            return findById(id, companyId);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Quotation> updateStatus(
            long id,
            long companyId,
            QuotationStatus status,
            Instant submittedAt,
            BigDecimal riskScore,
            RiskLevel riskLevel) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            if (submittedAt == null) {
                statement.setNull(2, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(2, Timestamp.from(submittedAt));
            }
            statement.setBigDecimal(3, riskScore);
            statement.setString(4, riskLevel.name());
            statement.setLong(5, id);
            statement.setLong(6, companyId);
            statement.executeUpdate();
            return findById(id, companyId);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void insertDismissal(long quotationId, long productId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT_DISMISSAL)) {
            statement.setLong(1, quotationId);
            statement.setLong(2, productId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Set<Long> findDismissedProductIds(long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_DISMISSALS)) {
            statement.setLong(1, quotationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> ids = new HashSet<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("product_id"));
                }
                return ids;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static Quotation map(ResultSet resultSet) throws SQLException {
        return new Quotation(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("quote_number"),
                resultSet.getLong("customer_id"),
                resultSet.getLong("sales_rep_id"),
                resultSet.getLong("price_list_id"),
                QuotationStatus.valueOf(resultSet.getString("status")),
                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("discount_amount"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getBigDecimal("total_cost"),
                resultSet.getBigDecimal("margin_amount"),
                resultSet.getBigDecimal("margin_percent"),
                resultSet.getBigDecimal("risk_score"),
                RiskLevel.valueOf(resultSet.getString("risk_level")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                nullableInstant(resultSet, "submitted_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}

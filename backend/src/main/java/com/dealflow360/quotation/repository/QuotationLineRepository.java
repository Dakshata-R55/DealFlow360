package com.dealflow360.quotation.repository;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.quotation.model.QuotationLine;
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
public class QuotationLineRepository {

    private static final String INSERT =
            """
            INSERT INTO quotation_lines (
              quotation_id, product_id, variant_id, quantity, base_unit_price, resolved_unit_price, cost_price,
              discount_percent, discount_amount, allowed_discount_percent, line_total, margin_amount, margin_percent,
              billing_type
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE =
            """
            UPDATE quotation_lines
            SET quantity = ?, discount_percent = ?, discount_amount = ?, allowed_discount_percent = ?,
                line_total = ?, margin_amount = ?, margin_percent = ?
            WHERE id = ? AND quotation_id = ?
            """;
    private static final String SELECT =
            """
            SELECT id, quotation_id, product_id, variant_id, quantity, base_unit_price, resolved_unit_price, cost_price,
                   discount_percent, discount_amount, allowed_discount_percent, line_total, margin_amount,
                   margin_percent, billing_type, created_at, updated_at
            FROM quotation_lines
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND quotation_id = ?";
    private static final String FIND_BY_QUOTATION = SELECT + " WHERE quotation_id = ? ORDER BY id";
    private static final String DELETE = "DELETE FROM quotation_lines WHERE id = ? AND quotation_id = ?";

    private final DataSource dataSource;

    public QuotationLineRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public QuotationLine insert(
            long quotationId,
            long productId,
            Long variantId,
            BigDecimal quantity,
            BigDecimal baseUnitPrice,
            BigDecimal resolvedUnitPrice,
            BigDecimal costPrice,
            BigDecimal discountPercent,
            BigDecimal discountAmount,
            BigDecimal allowedDiscountPercent,
            BigDecimal lineTotal,
            BigDecimal marginAmount,
            BigDecimal marginPercent,
            BillingType billingType) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, quotationId);
            statement.setLong(2, productId);
            if (variantId == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, variantId);
            }
            statement.setBigDecimal(4, quantity);
            statement.setBigDecimal(5, baseUnitPrice);
            statement.setBigDecimal(6, resolvedUnitPrice);
            statement.setBigDecimal(7, costPrice);
            statement.setBigDecimal(8, discountPercent);
            statement.setBigDecimal(9, discountAmount);
            statement.setBigDecimal(10, allowedDiscountPercent);
            statement.setBigDecimal(11, lineTotal);
            statement.setBigDecimal(12, marginAmount);
            statement.setBigDecimal(13, marginPercent);
            statement.setString(14, billingType.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert quotation line returned no id");
                }
                return findById(keys.getLong(1), quotationId)
                        .orElseThrow(() -> new SQLException("Inserted quotation line not found"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<QuotationLine> updateComputed(QuotationLine line) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setBigDecimal(1, line.quantity());
            statement.setBigDecimal(2, line.discountPercent());
            statement.setBigDecimal(3, line.discountAmount());
            statement.setBigDecimal(4, line.allowedDiscountPercent());
            statement.setBigDecimal(5, line.lineTotal());
            statement.setBigDecimal(6, line.marginAmount());
            statement.setBigDecimal(7, line.marginPercent());
            statement.setLong(8, line.id());
            statement.setLong(9, line.quotationId());
            statement.executeUpdate();
            return findById(line.id(), line.quotationId());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<QuotationLine> findById(long id, long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, id);
            statement.setLong(2, quotationId);
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

    public List<QuotationLine> findByQuotation(long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_QUOTATION)) {
            statement.setLong(1, quotationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuotationLine> rows = new ArrayList<>();
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

    public boolean delete(long id, long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setLong(1, id);
            statement.setLong(2, quotationId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static QuotationLine map(ResultSet resultSet) throws SQLException {
        long variantRaw = resultSet.getLong("variant_id");
        Long variantId = resultSet.wasNull() ? null : variantRaw;
        return new QuotationLine(
                resultSet.getLong("id"),
                resultSet.getLong("quotation_id"),
                resultSet.getLong("product_id"),
                variantId,
                resultSet.getBigDecimal("quantity"),
                resultSet.getBigDecimal("base_unit_price"),
                resultSet.getBigDecimal("resolved_unit_price"),
                resultSet.getBigDecimal("cost_price"),
                resultSet.getBigDecimal("discount_percent"),
                resultSet.getBigDecimal("discount_amount"),
                resultSet.getBigDecimal("allowed_discount_percent"),
                resultSet.getBigDecimal("line_total"),
                resultSet.getBigDecimal("margin_amount"),
                resultSet.getBigDecimal("margin_percent"),
                BillingType.valueOf(resultSet.getString("billing_type")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

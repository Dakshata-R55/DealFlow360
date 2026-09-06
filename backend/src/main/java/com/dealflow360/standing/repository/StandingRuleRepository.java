package com.dealflow360.standing.repository;

import com.dealflow360.standing.model.StandingRule;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class StandingRuleRepository {

    private static final String INSERT =
            """
            INSERT INTO standing_rules (company_id, customer_tier_id, min_spend, window_months)
            VALUES (?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, customer_tier_id, min_spend, window_months, created_at, updated_at
            FROM standing_rules
            """;
    private static final String UPDATE =
            """
            UPDATE standing_rules
            SET min_spend = ?, window_months = ?
            WHERE company_id = ? AND customer_tier_id = ?
            """;

    private final DataSource dataSource;

    public StandingRuleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public StandingRule insert(long companyId, long customerTierId, BigDecimal minSpend, int windowMonths) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setLong(2, customerTierId);
            statement.setBigDecimal(3, minSpend);
            statement.setInt(4, windowMonths);
            statement.executeUpdate();
            return findByTier(companyId, customerTierId)
                    .orElseThrow(() -> new SQLException("Inserted standing rule not found"));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<StandingRule> findByTier(long companyId, long customerTierId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(SELECT + " WHERE company_id = ? AND customer_tier_id = ?")) {
            statement.setLong(1, companyId);
            statement.setLong(2, customerTierId);
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

    public List<StandingRule> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(SELECT + " WHERE company_id = ? ORDER BY min_spend ASC, id ASC")) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StandingRule> rows = new ArrayList<>();
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

    public StandingRule upsert(long companyId, long customerTierId, BigDecimal minSpend, int windowMonths) {
        Optional<StandingRule> existing = findByTier(companyId, customerTierId);
        if (existing.isEmpty()) {
            return insert(companyId, customerTierId, minSpend, windowMonths);
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setBigDecimal(1, minSpend);
            statement.setInt(2, windowMonths);
            statement.setLong(3, companyId);
            statement.setLong(4, customerTierId);
            statement.executeUpdate();
            return findByTier(companyId, customerTierId)
                    .orElseThrow(() -> new SQLException("Updated standing rule not found"));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public BigDecimal confirmedSpend(long companyId, long customerId, int windowMonths) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT COALESCE(SUM(total_amount), 0)
                FROM quotations
                WHERE company_id = ? AND customer_id = ? AND status = 'CONFIRMED'
                  AND updated_at >= DATE_SUB(NOW(), INTERVAL ? MONTH)
                """)) {
            statement.setLong(1, companyId);
            statement.setLong(2, customerId);
            statement.setInt(3, windowMonths);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                BigDecimal value = resultSet.getBigDecimal(1);
                return value == null ? BigDecimal.ZERO : value;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static StandingRule map(ResultSet resultSet) throws SQLException {
        Timestamp created = resultSet.getTimestamp("created_at");
        Timestamp updated = resultSet.getTimestamp("updated_at");
        return new StandingRule(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getLong("customer_tier_id"),
                resultSet.getBigDecimal("min_spend"),
                resultSet.getInt("window_months"),
                created == null ? Instant.EPOCH : created.toInstant(),
                updated == null ? Instant.EPOCH : updated.toInstant());
    }
}

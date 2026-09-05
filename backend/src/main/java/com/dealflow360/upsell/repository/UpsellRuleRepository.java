package com.dealflow360.upsell.repository;

import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.upsell.model.UpsellRule;
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
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class UpsellRuleRepository {

    private static final String INSERT =
            """
            INSERT INTO upsell_rules (
              company_id, trigger_product_id, suggested_product_id, score, promotion_boost, min_margin_pct, active
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_BY_COMPANY =
            """
            SELECT id, company_id, trigger_product_id, suggested_product_id, score, promotion_boost, min_margin_pct,
                   active, created_at, updated_at
            FROM upsell_rules
            WHERE company_id = ?
            ORDER BY id
            """;

    private final DataSource dataSource;

    public UpsellRuleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UpsellRule insert(
            long companyId,
            long triggerProductId,
            long suggestedProductId,
            BigDecimal score,
            BigDecimal promotionBoost,
            BigDecimal minMarginPct,
            boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setLong(2, triggerProductId);
            statement.setLong(3, suggestedProductId);
            statement.setBigDecimal(4, score);
            statement.setBigDecimal(5, promotionBoost);
            statement.setBigDecimal(6, minMarginPct);
            statement.setBoolean(7, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert upsell rule returned no id");
                }
                return new UpsellRule(
                        keys.getLong(1),
                        companyId,
                        triggerProductId,
                        suggestedProductId,
                        score,
                        promotionBoost,
                        minMarginPct,
                        active,
                        Instant.now(),
                        Instant.now());
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Upsell rule already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<UpsellRule> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<UpsellRule> rows = new ArrayList<>();
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

    private static UpsellRule map(ResultSet resultSet) throws SQLException {
        return new UpsellRule(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getLong("trigger_product_id"),
                resultSet.getLong("suggested_product_id"),
                resultSet.getBigDecimal("score"),
                resultSet.getBigDecimal("promotion_boost"),
                resultSet.getBigDecimal("min_margin_pct"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

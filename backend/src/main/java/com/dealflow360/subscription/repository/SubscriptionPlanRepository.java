package com.dealflow360.subscription.repository;

import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.subscription.model.PlanCycle;
import com.dealflow360.subscription.model.SubscriptionPlan;
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
public class SubscriptionPlanRepository {

    private static final String INSERT =
            """
            INSERT INTO subscription_plans (company_id, name, cycle, proration_rule, cancellation_rule, active)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE =
            """
            UPDATE subscription_plans
            SET name = ?, cycle = ?, proration_rule = ?, cancellation_rule = ?, active = ?
            WHERE id = ? AND company_id = ?
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, name, cycle, proration_rule, cancellation_rule, active, created_at, updated_at
            FROM subscription_plans
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY name";

    private final DataSource dataSource;

    public SubscriptionPlanRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SubscriptionPlan insert(
            long companyId,
            String name,
            PlanCycle cycle,
            String prorationRule,
            String cancellationRule,
            boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
            statement.setString(3, cycle.name());
            statement.setString(4, prorationRule);
            statement.setString(5, cancellationRule);
            statement.setBoolean(6, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert subscription plan returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted subscription plan not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Subscription plan already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<SubscriptionPlan> update(SubscriptionPlan plan) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, plan.name());
            statement.setString(2, plan.cycle().name());
            statement.setString(3, plan.prorationRule());
            statement.setString(4, plan.cancellationRule());
            statement.setBoolean(5, plan.active());
            statement.setLong(6, plan.id());
            statement.setLong(7, plan.companyId());
            statement.executeUpdate();
            return findById(plan.id(), plan.companyId());
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Subscription plan already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<SubscriptionPlan> findById(long id, long companyId) {
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

    public List<SubscriptionPlan> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SubscriptionPlan> rows = new ArrayList<>();
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

    private static SubscriptionPlan map(ResultSet resultSet) throws SQLException {
        return new SubscriptionPlan(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("name"),
                PlanCycle.valueOf(resultSet.getString("cycle")),
                resultSet.getString("proration_rule"),
                resultSet.getString("cancellation_rule"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

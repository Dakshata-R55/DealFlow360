package com.dealflow360.pricing.repository;

import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.shared.exception.ConflictException;
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
public class CustomerTierRepository {

    private static final String INSERT =
            "INSERT INTO customer_tiers (company_id, name, default_discount_limit, active) VALUES (?, ?, ?, ?)";
    private static final String SELECT =
            """
            SELECT id, company_id, name, default_discount_limit, active, created_at, updated_at
            FROM customer_tiers
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY default_discount_limit";

    private final DataSource dataSource;

    public CustomerTierRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CustomerTier insert(long companyId, String name, BigDecimal defaultDiscountLimit, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
            statement.setBigDecimal(3, defaultDiscountLimit);
            statement.setBoolean(4, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert customer tier returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted customer tier not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Customer tier already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<CustomerTier> findById(long id, long companyId) {
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

    public List<CustomerTier> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CustomerTier> rows = new ArrayList<>();
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

    private static CustomerTier map(ResultSet resultSet) throws SQLException {
        return new CustomerTier(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("name"),
                resultSet.getBigDecimal("default_discount_limit"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

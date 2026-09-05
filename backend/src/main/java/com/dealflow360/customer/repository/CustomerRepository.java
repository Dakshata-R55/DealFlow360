package com.dealflow360.customer.repository;

import com.dealflow360.customer.model.Customer;
import com.dealflow360.shared.exception.ConflictException;
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
public class CustomerRepository {

    private static final String INSERT =
            "INSERT INTO customers (company_id, name, customer_tier_id, active) VALUES (?, ?, ?, ?)";
    private static final String SELECT =
            """
            SELECT id, company_id, name, customer_tier_id, active, created_at, updated_at
            FROM customers
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY name";
    private static final String FIND_BY_NAME = SELECT + " WHERE company_id = ? AND name = ?";

    private final DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Customer insert(long companyId, String name, long customerTierId, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
            statement.setLong(3, customerTierId);
            statement.setBoolean(4, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert customer returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted customer not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Customer already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Customer> findById(long id, long companyId) {
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

    public Optional<Customer> findByCompanyAndName(long companyId, String name) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_NAME)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
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

    public List<Customer> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Customer> rows = new ArrayList<>();
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

    private static Customer map(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("name"),
                resultSet.getLong("customer_tier_id"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

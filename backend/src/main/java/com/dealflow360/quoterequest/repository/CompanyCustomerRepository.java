package com.dealflow360.quoterequest.repository;

import com.dealflow360.quoterequest.model.CompanyCustomer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyCustomerRepository {

    private static final String INSERT =
            """
            INSERT INTO company_customers (
              seller_company_id, customer_user_id, customer_tier_id, seller_customer_id, status
            ) VALUES (?, ?, ?, ?, 'ACTIVE')
            """;
    private static final String SELECT =
            """
            SELECT id, seller_company_id, customer_user_id, customer_tier_id, seller_customer_id, status,
                   created_at, updated_at
            FROM company_customers
            """;

    private final DataSource dataSource;

    public CompanyCustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CompanyCustomer insert(
            long sellerCompanyId, long customerUserId, long customerTierId, long sellerCustomerId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, sellerCompanyId);
            statement.setLong(2, customerUserId);
            statement.setLong(3, customerTierId);
            statement.setLong(4, sellerCustomerId);
            statement.executeUpdate();
            return find(sellerCompanyId, customerUserId)
                    .orElseThrow(() -> new SQLException("Inserted company customer not found"));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<CompanyCustomer> find(long sellerCompanyId, long customerUserId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(SELECT + " WHERE seller_company_id = ? AND customer_user_id = ?")) {
            statement.setLong(1, sellerCompanyId);
            statement.setLong(2, customerUserId);
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

    private static CompanyCustomer map(ResultSet resultSet) throws SQLException {
        return new CompanyCustomer(
                resultSet.getLong("id"),
                resultSet.getLong("seller_company_id"),
                resultSet.getLong("customer_user_id"),
                resultSet.getLong("customer_tier_id"),
                resultSet.getLong("seller_customer_id"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

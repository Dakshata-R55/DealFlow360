package com.dealflow360.catalog.repository;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.catalog.model.Product;
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
public class ProductRepository {

    private static final String INSERT =
            """
            INSERT INTO products (
              company_id, category_id, name, description, unit, base_price, cost_price, tax_percent, billing_type, active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE =
            """
            UPDATE products
            SET category_id = ?, name = ?, description = ?, unit = ?, base_price = ?, cost_price = ?,
                tax_percent = ?, billing_type = ?, active = ?
            WHERE id = ? AND company_id = ?
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, category_id, name, description, unit, base_price, cost_price, tax_percent,
                   billing_type, active, created_at, updated_at
            FROM products
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY name";

    private final DataSource dataSource;

    public ProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Product insert(
            long companyId,
            long categoryId,
            String name,
            String description,
            String unit,
            BigDecimal basePrice,
            BigDecimal costPrice,
            BigDecimal taxPercent,
            BillingType billingType,
            boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setLong(2, categoryId);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setString(5, unit);
            statement.setBigDecimal(6, basePrice);
            statement.setBigDecimal(7, costPrice);
            statement.setBigDecimal(8, taxPercent);
            statement.setString(9, billingType.name());
            statement.setBoolean(10, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert product returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted product not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Product already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Product> update(Product product) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setLong(1, product.categoryId());
            statement.setString(2, product.name());
            statement.setString(3, product.description());
            statement.setString(4, product.unit());
            statement.setBigDecimal(5, product.basePrice());
            statement.setBigDecimal(6, product.costPrice());
            statement.setBigDecimal(7, product.taxPercent());
            statement.setString(8, product.billingType().name());
            statement.setBoolean(9, product.active());
            statement.setLong(10, product.id());
            statement.setLong(11, product.companyId());
            statement.executeUpdate();
            return findById(product.id(), product.companyId());
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Product already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Product> findById(long id, long companyId) {
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

    public List<Product> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Product> rows = new ArrayList<>();
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

    private static Product map(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getLong("category_id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("unit"),
                resultSet.getBigDecimal("base_price"),
                resultSet.getBigDecimal("cost_price"),
                resultSet.getBigDecimal("tax_percent"),
                BillingType.valueOf(resultSet.getString("billing_type")),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

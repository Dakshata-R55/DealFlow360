package com.dealflow360.catalog.repository;

import com.dealflow360.catalog.model.ProductVariant;
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
public class ProductVariantRepository {

    private static final String INSERT =
            """
            INSERT INTO product_variants (product_id, attribute_name, attribute_value, extra_price)
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE =
            """
            UPDATE product_variants v
            JOIN products p ON p.id = v.product_id
            SET v.attribute_name = ?, v.attribute_value = ?, v.extra_price = ?
            WHERE v.id = ? AND v.product_id = ? AND p.company_id = ?
            """;
    private static final String SELECT =
            """
            SELECT v.id, v.product_id, v.attribute_name, v.attribute_value, v.extra_price, v.created_at, v.updated_at
            FROM product_variants v
            JOIN products p ON p.id = v.product_id
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE v.id = ? AND p.company_id = ?";
    private static final String FIND_BY_PRODUCT = SELECT + " WHERE v.product_id = ? AND p.company_id = ? ORDER BY v.id";

    private final DataSource dataSource;

    public ProductVariantRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProductVariant insert(
            long companyId, long productId, String attributeName, String attributeValue, BigDecimal extraPrice) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, productId);
            statement.setString(2, attributeName);
            statement.setString(3, attributeValue);
            statement.setBigDecimal(4, extraPrice);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert variant returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted variant not found"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<ProductVariant> update(
            long companyId,
            long productId,
            long variantId,
            String attributeName,
            String attributeValue,
            BigDecimal extraPrice) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, attributeName);
            statement.setString(2, attributeValue);
            statement.setBigDecimal(3, extraPrice);
            statement.setLong(4, variantId);
            statement.setLong(5, productId);
            statement.setLong(6, companyId);
            statement.executeUpdate();
            return findById(variantId, companyId);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<ProductVariant> findById(long id, long companyId) {
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

    public List<ProductVariant> findByProduct(long productId, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_PRODUCT)) {
            statement.setLong(1, productId);
            statement.setLong(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProductVariant> rows = new ArrayList<>();
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

    private static ProductVariant map(ResultSet resultSet) throws SQLException {
        return new ProductVariant(
                resultSet.getLong("id"),
                resultSet.getLong("product_id"),
                resultSet.getString("attribute_name"),
                resultSet.getString("attribute_value"),
                resultSet.getBigDecimal("extra_price"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

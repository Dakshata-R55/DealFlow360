package com.dealflow360.pricing.repository;

import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.model.PriceListItem;
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
public class PriceListRepository {

    private static final String INSERT =
            """
            INSERT INTO price_lists (company_id, name, currency, customer_tier_id, active)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, name, currency, customer_tier_id, active, created_at, updated_at
            FROM price_lists
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY name";
    private static final String UPSERT_ITEM =
            """
            INSERT INTO price_list_items (price_list_id, product_id, price)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE price = VALUES(price)
            """;
    private static final String FIND_ITEMS =
            """
            SELECT i.price_list_id, i.product_id, i.price
            FROM price_list_items i
            JOIN price_lists pl ON pl.id = i.price_list_id
            WHERE i.price_list_id = ? AND pl.company_id = ?
            ORDER BY i.product_id
            """;

    private final DataSource dataSource;

    public PriceListRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PriceList insert(long companyId, String name, String currency, long customerTierId, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
            statement.setString(3, currency);
            statement.setLong(4, customerTierId);
            statement.setBoolean(5, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert price list returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted price list not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Price list already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<PriceList> findById(long id, long companyId) {
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

    public List<PriceList> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PriceList> rows = new ArrayList<>();
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

    public PriceListItem upsertItem(long priceListId, long productId, BigDecimal price) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_ITEM)) {
            statement.setLong(1, priceListId);
            statement.setLong(2, productId);
            statement.setBigDecimal(3, price);
            statement.executeUpdate();
            return new PriceListItem(priceListId, productId, price);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<PriceListItem> findItems(long priceListId, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_ITEMS)) {
            statement.setLong(1, priceListId);
            statement.setLong(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PriceListItem> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new PriceListItem(
                            resultSet.getLong("price_list_id"),
                            resultSet.getLong("product_id"),
                            resultSet.getBigDecimal("price")));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static PriceList map(ResultSet resultSet) throws SQLException {
        return new PriceList(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("name"),
                resultSet.getString("currency"),
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

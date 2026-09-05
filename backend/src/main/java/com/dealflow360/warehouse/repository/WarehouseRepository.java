package com.dealflow360.warehouse.repository;

import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.warehouse.model.Warehouse;
import com.dealflow360.warehouse.model.WarehouseInventory;
import com.dealflow360.warehouse.model.WarehouseStock;
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
public class WarehouseRepository {

    private static final String INSERT =
            """
            INSERT INTO warehouses (company_id, name, location, shipping_cost_weight, active)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, name, location, shipping_cost_weight, active, created_at, updated_at
            FROM warehouses
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE id = ? AND company_id = ?";
    private static final String FIND_BY_COMPANY = SELECT + " WHERE company_id = ? ORDER BY name";
    private static final String UPSERT_INVENTORY =
            """
            INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved, min_stock, reorder_qty)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              on_hand = VALUES(on_hand),
              reserved = VALUES(reserved),
              min_stock = VALUES(min_stock),
              reorder_qty = VALUES(reorder_qty)
            """;
    private static final String FIND_INVENTORY =
            """
            SELECT i.warehouse_id, i.product_id, i.on_hand, i.reserved, i.min_stock, i.reorder_qty
            FROM warehouse_inventory i
            JOIN warehouses w ON w.id = i.warehouse_id
            WHERE i.warehouse_id = ? AND w.company_id = ?
            ORDER BY i.product_id
            """;

    private static final String FIND_ACTIVE_STOCK =
            """
            SELECT w.id AS warehouse_id, w.name, w.shipping_cost_weight,
                   COALESCE(i.on_hand, 0) AS on_hand, COALESCE(i.reserved, 0) AS reserved,
                   i.product_id IS NOT NULL AS has_row
            FROM warehouses w
            LEFT JOIN warehouse_inventory i ON i.warehouse_id = w.id AND i.product_id = ?
            WHERE w.company_id = ? AND w.active = 1
            ORDER BY w.shipping_cost_weight ASC, w.id ASC
            """;
    private static final String FIND_ACTIVE_STOCK_LOCK = FIND_ACTIVE_STOCK + " FOR UPDATE";
    private static final String ADD_RESERVED =
            """
            UPDATE warehouse_inventory
            SET reserved = reserved + ?
            WHERE warehouse_id = ? AND product_id = ? AND on_hand - reserved >= ?
            """;
    private static final String RELEASE_RESERVED =
            """
            UPDATE warehouse_inventory
            SET reserved = reserved + ?
            WHERE warehouse_id = ? AND product_id = ? AND reserved >= ?
            """;

    private final DataSource dataSource;

    public WarehouseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Warehouse insert(
            long companyId, String name, String location, BigDecimal shippingCostWeight, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, name);
            statement.setString(3, location);
            statement.setBigDecimal(4, shippingCostWeight);
            statement.setBoolean(5, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert warehouse returned no id");
                }
                return findById(keys.getLong(1), companyId)
                        .orElseThrow(() -> new SQLException("Inserted warehouse not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Warehouse already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Warehouse> findById(long id, long companyId) {
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

    public List<Warehouse> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Warehouse> rows = new ArrayList<>();
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

    public WarehouseInventory upsertInventory(
            long warehouseId, long productId, int onHand, int reserved, int minStock, int reorderQty) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_INVENTORY)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setInt(3, onHand);
            statement.setInt(4, reserved);
            statement.setInt(5, minStock);
            statement.setInt(6, reorderQty);
            statement.executeUpdate();
            return new WarehouseInventory(warehouseId, productId, onHand, reserved, minStock, reorderQty);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<WarehouseStock> findActiveStockForProduct(long companyId, long productId) {
        return findActiveStockForProduct(companyId, productId, false);
    }

    public List<WarehouseStock> lockActiveStockForProduct(long companyId, long productId) {
        return findActiveStockForProduct(companyId, productId, true);
    }

    private List<WarehouseStock> findActiveStockForProduct(long companyId, long productId, boolean lock) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(lock ? FIND_ACTIVE_STOCK_LOCK : FIND_ACTIVE_STOCK)) {
            statement.setLong(1, productId);
            statement.setLong(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WarehouseStock> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapStock(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void addReserved(long warehouseId, long productId, int delta) {
        if (delta == 0) {
            return;
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement(delta > 0 ? ADD_RESERVED : RELEASE_RESERVED)) {
            statement.setInt(1, delta);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            if (delta > 0) {
                statement.setInt(4, delta);
            } else {
                statement.setInt(4, -delta);
            }
            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new ConflictException(
                        delta > 0 ? "Not enough available stock to reserve" : "Could not release reservation");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<WarehouseInventory> findInventory(long warehouseId, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_INVENTORY)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WarehouseInventory> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapInventory(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static WarehouseStock mapStock(ResultSet resultSet) throws SQLException {
        return new WarehouseStock(
                resultSet.getLong("warehouse_id"),
                resultSet.getString("name"),
                resultSet.getBigDecimal("shipping_cost_weight"),
                resultSet.getInt("on_hand"),
                resultSet.getInt("reserved"),
                resultSet.getBoolean("has_row"));
    }

    private static Warehouse map(ResultSet resultSet) throws SQLException {
        return new Warehouse(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getString("name"),
                resultSet.getString("location"),
                resultSet.getBigDecimal("shipping_cost_weight"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static WarehouseInventory mapInventory(ResultSet resultSet) throws SQLException {
        return new WarehouseInventory(
                resultSet.getLong("warehouse_id"),
                resultSet.getLong("product_id"),
                resultSet.getInt("on_hand"),
                resultSet.getInt("reserved"),
                resultSet.getInt("min_stock"),
                resultSet.getInt("reorder_qty"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

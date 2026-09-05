package com.dealflow360.fulfillment.repository;

import com.dealflow360.fulfillment.model.AllocationKind;
import com.dealflow360.fulfillment.model.AllocationSource;
import com.dealflow360.fulfillment.model.FulfillmentAllocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class FulfillmentAllocationRepository {

    private static final String INSERT =
            """
            INSERT INTO fulfillment_allocations (
              company_id, quotation_id, quotation_line_id, warehouse_id, quantity, kind, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT =
            """
            SELECT id, company_id, quotation_id, quotation_line_id, warehouse_id, quantity, kind, source, created_at
            FROM fulfillment_allocations
            """;
    private static final String FIND_BY_QUOTE = SELECT + " WHERE company_id = ? AND quotation_id = ? ORDER BY id";
    private static final String DELETE_BY_QUOTE =
            "DELETE FROM fulfillment_allocations WHERE company_id = ? AND quotation_id = ?";
    private static final String DELETE_BY_LINE =
            "DELETE FROM fulfillment_allocations WHERE company_id = ? AND quotation_id = ? AND quotation_line_id = ?";

    private final DataSource dataSource;

    public FulfillmentAllocationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public FulfillmentAllocation insert(
            long companyId,
            long quotationId,
            long quotationLineId,
            Long warehouseId,
            int quantity,
            AllocationKind kind,
            AllocationSource source) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setLong(2, quotationId);
            statement.setLong(3, quotationLineId);
            if (warehouseId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, warehouseId);
            }
            statement.setInt(5, quantity);
            statement.setString(6, kind.name());
            statement.setString(7, source.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert allocation returned no id");
                }
                long id = keys.getLong(1);
                return new FulfillmentAllocation(
                        id, companyId, quotationId, quotationLineId, warehouseId, quantity, kind, source, Instant.now());
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<FulfillmentAllocation> findByQuotation(long companyId, long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_QUOTE)) {
            statement.setLong(1, companyId);
            statement.setLong(2, quotationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FulfillmentAllocation> rows = new ArrayList<>();
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

    public void deleteByQuotation(long companyId, long quotationId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_QUOTE)) {
            statement.setLong(1, companyId);
            statement.setLong(2, quotationId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void deleteByLine(long companyId, long quotationId, long lineId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_LINE)) {
            statement.setLong(1, companyId);
            statement.setLong(2, quotationId);
            statement.setLong(3, lineId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static FulfillmentAllocation map(ResultSet resultSet) throws SQLException {
        long warehouse = resultSet.getLong("warehouse_id");
        Long warehouseId = resultSet.wasNull() ? null : warehouse;
        Timestamp created = resultSet.getTimestamp("created_at");
        return new FulfillmentAllocation(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getLong("quotation_id"),
                resultSet.getLong("quotation_line_id"),
                warehouseId,
                resultSet.getInt("quantity"),
                AllocationKind.valueOf(resultSet.getString("kind")),
                AllocationSource.valueOf(resultSet.getString("source")),
                created == null ? Instant.EPOCH : created.toInstant());
    }
}

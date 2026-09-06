package com.dealflow360.dashboard.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    public record CountRow(String key, long count) {}

    public record QuoteRow(
            long id,
            String quoteNumber,
            String customerName,
            BigDecimal totalAmount,
            String status,
            Instant createdAt,
            Instant updatedAt) {}

    public record ProductRow(long id, String name, String categoryName, BigDecimal basePrice, String billingType, Instant updatedAt) {}

    public record WarehouseRow(long id, String name, String location, Instant updatedAt) {}

    public record NamedStamp(long id, String name, Instant updatedAt) {}

    public record RequestRow(long id, String requestNumber, String customerName, String status, Instant updatedAt) {}

    private final DataSource dataSource;

    public DashboardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long countQuotes(long companyId) {
        return scalar("SELECT COUNT(*) FROM quotations WHERE company_id = ?", companyId);
    }

    public long countQuotesByStatus(long companyId, String status) {
        return scalar("SELECT COUNT(*) FROM quotations WHERE company_id = ? AND status = ?", companyId, status);
    }

    public BigDecimal confirmedRevenueThisMonth(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT COALESCE(SUM(total_amount), 0)
                FROM quotations
                WHERE company_id = ? AND status = 'CONFIRMED'
                  AND YEAR(updated_at) = YEAR(CURDATE()) AND MONTH(updated_at) = MONTH(CURDATE())
                """)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public long countActiveCustomers(long companyId) {
        return scalar("SELECT COUNT(*) FROM customers WHERE company_id = ? AND active = 1", companyId);
    }

    public List<CountRow> quoteCountsByStatus(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status, COUNT(*) AS n FROM quotations WHERE company_id = ? GROUP BY status")) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new CountRow(resultSet.getString("status"), resultSet.getLong("n")));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<QuoteRow> recentQuotes(long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT q.id, q.quote_number, c.name AS customer_name, q.total_amount, q.status, q.created_at, q.updated_at
                FROM quotations q
                JOIN customers c ON c.id = q.customer_id
                WHERE q.company_id = ?
                ORDER BY q.updated_at DESC, q.id DESC
                LIMIT ?
                """)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuoteRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapQuote(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public long countProducts(long companyId) {
        return scalar("SELECT COUNT(*) FROM products WHERE company_id = ?", companyId);
    }

    public long countCategories(long companyId) {
        return scalar("SELECT COUNT(*) FROM product_categories WHERE company_id = ?", companyId);
    }

    public long countWarehouses(long companyId) {
        return scalar("SELECT COUNT(*) FROM warehouses WHERE company_id = ?", companyId);
    }

    public long countPlans(long companyId) {
        return scalar("SELECT COUNT(*) FROM subscription_plans WHERE company_id = ?", companyId);
    }

    public List<CountRow> productCountsByCategory(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT cat.name AS key_name, COUNT(p.id) AS n
                FROM product_categories cat
                LEFT JOIN products p ON p.category_id = cat.id
                WHERE cat.company_id = ?
                GROUP BY cat.id, cat.name
                ORDER BY cat.name
                """)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new CountRow(resultSet.getString("key_name"), resultSet.getLong("n")));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<ProductRow> recentProducts(long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT p.id, p.name, cat.name AS category_name, p.base_price, p.billing_type, p.updated_at
                FROM products p
                JOIN product_categories cat ON cat.id = p.category_id
                WHERE p.company_id = ?
                ORDER BY p.updated_at DESC, p.id DESC
                LIMIT ?
                """)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProductRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp updated = resultSet.getTimestamp("updated_at");
                    rows.add(new ProductRow(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getString("category_name"),
                            resultSet.getBigDecimal("base_price"),
                            resultSet.getString("billing_type"),
                            updated == null ? Instant.EPOCH : updated.toInstant()));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<NamedStamp> recentCategories(long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, name, updated_at
                FROM product_categories
                WHERE company_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<NamedStamp> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp updated = resultSet.getTimestamp("updated_at");
                    rows.add(new NamedStamp(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            updated == null ? Instant.EPOCH : updated.toInstant()));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<WarehouseRow> recentWarehouses(long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, name, location, updated_at
                FROM warehouses
                WHERE company_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WarehouseRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp updated = resultSet.getTimestamp("updated_at");
                    rows.add(new WarehouseRow(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getString("location"),
                            updated == null ? Instant.EPOCH : updated.toInstant()));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<QuoteRow> searchQuotes(long companyId, String q, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT q.id, q.quote_number, c.name AS customer_name, q.total_amount, q.status, q.created_at, q.updated_at
                FROM quotations q
                JOIN customers c ON c.id = q.customer_id
                WHERE q.company_id = ? AND (q.quote_number LIKE ? OR c.name LIKE ?)
                ORDER BY q.updated_at DESC
                LIMIT ?
                """)) {
            String like = like(q);
            statement.setLong(1, companyId);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuoteRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapQuote(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<CountRow> searchNamed(
            String sql, long companyId, String q, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, companyId);
            statement.setString(2, like(q));
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new CountRow(resultSet.getString("label"), resultSet.getLong("id")));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<NamedStamp> recentCustomers(long companyId, int limit) {
        return namedStamps(
                "SELECT id, name, updated_at FROM customers WHERE company_id = ? ORDER BY updated_at DESC, id DESC LIMIT ?",
                companyId,
                limit);
    }

    public List<NamedStamp> recentPlans(long companyId, int limit) {
        return namedStamps(
                """
                SELECT id, name, updated_at
                FROM subscription_plans
                WHERE company_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """,
                companyId,
                limit);
    }

    public List<RequestRow> recentRequests(long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT r.id, r.request_number, u.name AS customer_name, r.status, r.updated_at
                FROM quote_requests r
                JOIN users u ON u.id = r.customer_user_id
                WHERE r.seller_company_id = ?
                ORDER BY r.updated_at DESC, r.id DESC
                LIMIT ?
                """)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp updated = resultSet.getTimestamp("updated_at");
                    rows.add(new RequestRow(
                            resultSet.getLong("id"),
                            resultSet.getString("request_number"),
                            resultSet.getString("customer_name"),
                            resultSet.getString("status"),
                            updated == null ? Instant.EPOCH : updated.toInstant()));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private List<NamedStamp> namedStamps(String sql, long companyId, int limit) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, companyId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<NamedStamp> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp updated = resultSet.getTimestamp("updated_at");
                    rows.add(new NamedStamp(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            updated == null ? Instant.EPOCH : updated.toInstant()));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static QuoteRow mapQuote(ResultSet resultSet) throws SQLException {
        Timestamp created = resultSet.getTimestamp("created_at");
        Timestamp updated = resultSet.getTimestamp("updated_at");
        return new QuoteRow(
                resultSet.getLong("id"),
                resultSet.getString("quote_number"),
                resultSet.getString("customer_name"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getString("status"),
                created == null ? Instant.EPOCH : created.toInstant(),
                updated == null ? Instant.EPOCH : updated.toInstant());
    }

    private long scalar(String sql, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private long scalar(String sql, long companyId, String status) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, companyId);
            statement.setString(2, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static String like(String q) {
        return "%" + q.replace("%", "\\%").replace("_", "\\_") + "%";
    }
}

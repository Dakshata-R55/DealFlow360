package com.dealflow360.auth.repository;

import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;
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
public class UserRepository {

    private static final String INSERT =
            """
            INSERT INTO users (company_id, name, email, password_hash, role, active)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_BY_ID =
            """
            SELECT id, company_id, name, email, password_hash, role, active, created_at, updated_at
            FROM users
            WHERE id = ?
            """;
    private static final String FIND_BY_EMAIL =
            """
            SELECT id, company_id, name, email, password_hash, role, active, created_at, updated_at
            FROM users
            WHERE email = ?
            """;
    private static final String EXISTS_BY_EMAIL = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
    private static final String FIND_ACTIVE_SALES_BY_COMPANY =
            """
            SELECT id, company_id, name, email, password_hash, role, active, created_at, updated_at
            FROM users
            WHERE company_id = ?
              AND active = 1
              AND role IN ('SALES_REP', 'SALES_MANAGER', 'FINANCE_OPS')
            ORDER BY name, id
            """;
    private static final String FIND_INTERNAL_BY_COMPANY =
            """
            SELECT id, company_id, name, email, password_hash, role, active, created_at, updated_at
            FROM users
            WHERE company_id = ?
              AND role IN ('ADMIN', 'SALES_REP', 'SALES_MANAGER', 'FINANCE_OPS')
            ORDER BY name, id
            """;
    private static final String FIND_BY_ID_AND_COMPANY =
            """
            SELECT id, company_id, name, email, password_hash, role, active, created_at, updated_at
            FROM users
            WHERE id = ? AND company_id = ?
            """;
    private static final String UPDATE_ACTIVE =
            """
            UPDATE users
            SET active = ?
            WHERE id = ? AND company_id = ?
            """;

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User insert(
            long companyId, String name, String email, String passwordHash, UserRole role, boolean active) {
        return insert(Long.valueOf(companyId), name, email, passwordHash, role, active);
    }

    public User insert(
            Long companyId, String name, String email, String passwordHash, UserRole role, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            if (companyId == null) {
                statement.setObject(1, null);
            } else {
                statement.setLong(1, companyId);
            }
            statement.setString(2, name);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, role.name());
            statement.setBoolean(6, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert user returned no id");
                }
                long id = keys.getLong(1);
                return findById(id).orElseThrow(() -> new SQLException("Inserted user not found"));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Email already exists");
            }
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<User> findById(long id) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, id);
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

    public Optional<User> findByEmail(String email) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL)) {
            statement.setString(1, email);
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

    public List<User> findActiveSalesByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_SALES_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> rows = new ArrayList<>();
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

    public List<User> findInternalByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_INTERNAL_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> rows = new ArrayList<>();
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

    public Optional<User> findByIdAndCompany(long id, long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_AND_COMPANY)) {
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

    public void updateActive(long id, long companyId, boolean active) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_ACTIVE)) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            statement.setLong(3, companyId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RuntimeException("User not found");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public boolean existsByEmail(String email) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(EXISTS_BY_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static User map(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                nullableLong(resultSet, "company_id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}
package com.dealflow360.company.repository;

import com.dealflow360.company.model.Company;
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
public class CompanyRepository {

    private static final String SELECT =
            "SELECT id, name, code, description, active, created_at, updated_at FROM companies";
    private static final String INSERT =
            "INSERT INTO companies (name, code, description, active) VALUES (?, ?, ?, 1)";
    private static final String UPDATE_DISPLAY = "UPDATE companies SET name = ?, description = ? WHERE id = ?";
    private static final String FIND_BY_ID = SELECT + " WHERE id = ?";
    private static final String FIND_BY_CODE = SELECT + " WHERE code = ?";
    private static final String EXISTS_BY_CODE = "SELECT 1 FROM companies WHERE code = ? LIMIT 1";
    private static final String FIND_ACTIVE =
            SELECT + " WHERE active = 1 AND (? = '' OR name LIKE ?) ORDER BY name";

    private final DataSource dataSource;

    public CompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Company insert(String name, String code) {
        return insert(name, code, "");
    }

    public Company insert(String name, String code, String description) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, code);
            statement.setString(3, description == null ? "" : description);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert company returned no id");
                }
                long id = keys.getLong(1);
                return findById(id).orElseThrow(() -> new SQLException("Inserted company not found"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void updateDisplay(long id, String name, String description) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_DISPLAY)) {
            statement.setString(1, name);
            statement.setString(2, description == null ? "" : description);
            statement.setLong(3, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Optional<Company> findById(long id) {
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

    public Optional<Company> findByCode(String code) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CODE)) {
            statement.setString(1, code);
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

    public List<Company> findActive(String query) {
        String needle = query == null ? "" : query.trim();
        String like = "%" + needle + "%";
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE)) {
            statement.setString(1, needle);
            statement.setString(2, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Company> rows = new ArrayList<>();
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

    public boolean existsByCode(String code) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(EXISTS_BY_CODE)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static Company map(ResultSet resultSet) throws SQLException {
        String description = resultSet.getString("description");
        return new Company(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("code"),
                description == null ? "" : description,
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}

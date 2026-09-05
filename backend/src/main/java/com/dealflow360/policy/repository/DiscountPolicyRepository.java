package com.dealflow360.policy.repository;

import com.dealflow360.policy.model.DiscountPolicy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class DiscountPolicyRepository {

    private static final String INSERT =
            """
            INSERT INTO discount_policies (company_id, customer_tier_id, category_id, max_discount_pct)
            VALUES (?, ?, ?, ?)
            """;
    private static final String DELETE_BY_COMPANY = "DELETE FROM discount_policies WHERE company_id = ?";
    private static final String FIND_BY_COMPANY =
            """
            SELECT id, company_id, customer_tier_id, category_id, max_discount_pct
            FROM discount_policies
            WHERE company_id = ?
            ORDER BY id
            """;

    private final DataSource dataSource;

    public DiscountPolicyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DiscountPolicy insert(long companyId, Long customerTierId, Long categoryId, BigDecimal maxDiscountPct) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            setNullableLong(statement, 2, customerTierId);
            setNullableLong(statement, 3, categoryId);
            statement.setBigDecimal(4, maxDiscountPct);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert discount policy returned no id");
                }
                return new DiscountPolicy(keys.getLong(1), companyId, customerTierId, categoryId, maxDiscountPct);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void deleteByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_COMPANY)) {
            statement.setLong(1, companyId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<DiscountPolicy> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DiscountPolicy> rows = new ArrayList<>();
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

    private static DiscountPolicy map(ResultSet resultSet) throws SQLException {
        return new DiscountPolicy(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                nullableLong(resultSet, "customer_tier_id"),
                nullableLong(resultSet, "category_id"),
                resultSet.getBigDecimal("max_discount_pct"));
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}

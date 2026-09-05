package com.dealflow360.policy.repository;

import com.dealflow360.policy.model.ApprovalPolicy;
import com.dealflow360.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalPolicyRepository {

    private static final String INSERT =
            """
            INSERT INTO approval_policies (
              company_id, manager_line_excess_pct, finance_line_excess_pct,
              manager_quote_excess_pct, finance_quote_excess_pct
            ) VALUES (?, ?, ?, ?, ?)
            """;
    private static final String DELETE_BY_COMPANY = "DELETE FROM approval_policies WHERE company_id = ?";
    private static final String FIND_BY_COMPANY =
            """
            SELECT id, company_id, manager_line_excess_pct, finance_line_excess_pct,
                   manager_quote_excess_pct, finance_quote_excess_pct
            FROM approval_policies
            WHERE company_id = ?
            LIMIT 1
            """;

    private final DataSource dataSource;

    public ApprovalPolicyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ApprovalPolicy insert(
            long companyId,
            BigDecimal managerLineExcessPercent,
            BigDecimal financeLineExcessPercent,
            BigDecimal managerQuoteExcessPercent,
            BigDecimal financeQuoteExcessPercent) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setBigDecimal(2, managerLineExcessPercent);
            statement.setBigDecimal(3, financeLineExcessPercent);
            statement.setBigDecimal(4, managerQuoteExcessPercent);
            statement.setBigDecimal(5, financeQuoteExcessPercent);
            statement.executeUpdate();
            return findByCompany(companyId).orElseThrow(() -> new SQLException("Inserted approval policy not found"));
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new ConflictException("Approval policy already exists");
            }
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

    public Optional<ApprovalPolicy> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
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

    private static ApprovalPolicy map(ResultSet resultSet) throws SQLException {
        return new ApprovalPolicy(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                resultSet.getBigDecimal("manager_line_excess_pct"),
                resultSet.getBigDecimal("finance_line_excess_pct"),
                resultSet.getBigDecimal("manager_quote_excess_pct"),
                resultSet.getBigDecimal("finance_quote_excess_pct"));
    }
}

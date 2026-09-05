package com.dealflow360.policy.repository;

import com.dealflow360.policy.model.ApprovalPolicy;
import com.dealflow360.policy.model.RiskLevel;
import com.dealflow360.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalPolicyRepository {

    private static final String INSERT =
            """
            INSERT INTO approval_policies (
              company_id, risk_level, min_score, max_score, requires_manager, requires_finance, hard_line_excess_threshold
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String DELETE_BY_COMPANY = "DELETE FROM approval_policies WHERE company_id = ?";
    private static final String FIND_BY_COMPANY =
            """
            SELECT id, company_id, risk_level, min_score, max_score, requires_manager, requires_finance,
                   hard_line_excess_threshold
            FROM approval_policies
            WHERE company_id = ?
            ORDER BY min_score
            """;

    private final DataSource dataSource;

    public ApprovalPolicyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ApprovalPolicy insert(
            long companyId,
            RiskLevel riskLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            boolean requiresManager,
            boolean requiresFinance,
            BigDecimal hardLineExcessThreshold) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, companyId);
            statement.setString(2, riskLevel.name());
            statement.setBigDecimal(3, minScore);
            statement.setBigDecimal(4, maxScore);
            statement.setBoolean(5, requiresManager);
            statement.setBoolean(6, requiresFinance);
            statement.setBigDecimal(7, hardLineExcessThreshold);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert approval policy returned no id");
                }
                return new ApprovalPolicy(
                        keys.getLong(1),
                        companyId,
                        riskLevel,
                        minScore,
                        maxScore,
                        requiresManager,
                        requiresFinance,
                        hardLineExcessThreshold);
            }
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

    public List<ApprovalPolicy> findByCompany(long companyId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY)) {
            statement.setLong(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ApprovalPolicy> rows = new ArrayList<>();
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

    private static ApprovalPolicy map(ResultSet resultSet) throws SQLException {
        return new ApprovalPolicy(
                resultSet.getLong("id"),
                resultSet.getLong("company_id"),
                RiskLevel.valueOf(resultSet.getString("risk_level")),
                resultSet.getBigDecimal("min_score"),
                resultSet.getBigDecimal("max_score"),
                resultSet.getBoolean("requires_manager"),
                resultSet.getBoolean("requires_finance"),
                resultSet.getBigDecimal("hard_line_excess_threshold"));
    }
}

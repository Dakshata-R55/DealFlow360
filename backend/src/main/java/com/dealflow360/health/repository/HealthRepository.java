package com.dealflow360.health.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class HealthRepository {

    private static final String PING = "SELECT 1";

    private final DataSource dataSource;

    public HealthRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void ping() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(PING);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("SELECT 1 returned no row");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}

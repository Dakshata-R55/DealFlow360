package com.dealflow360.quoterequest.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class QuoteRequestDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuoteRequestDataInitializer.class);

    private static final String SEQUENCES_SQL =
            """
            CREATE TABLE IF NOT EXISTS quote_request_number_sequences (
              year INT NOT NULL,
              last_number INT NOT NULL,
              PRIMARY KEY (year)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String REQUESTS_SQL =
            """
            CREATE TABLE IF NOT EXISTS quote_requests (
              id BIGINT NOT NULL AUTO_INCREMENT,
              request_number VARCHAR(32) NOT NULL,
              customer_user_id BIGINT NOT NULL,
              seller_company_id BIGINT NOT NULL,
              status VARCHAR(32) NOT NULL,
              requested_delivery_date DATE NULL,
              target_budget DECIMAL(14,2) NULL,
              expected_discount_percent DECIMAL(7,4) NULL,
              notes VARCHAR(2000) NOT NULL DEFAULT '',
              quotation_id BIGINT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              submitted_at TIMESTAMP NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_quote_requests_number (request_number),
              KEY idx_quote_requests_customer (customer_user_id),
              KEY idx_quote_requests_seller (seller_company_id),
              CONSTRAINT fk_quote_requests_customer FOREIGN KEY (customer_user_id) REFERENCES users (id),
              CONSTRAINT fk_quote_requests_seller FOREIGN KEY (seller_company_id) REFERENCES companies (id),
              CONSTRAINT fk_quote_requests_quotation FOREIGN KEY (quotation_id) REFERENCES quotations (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String LINES_SQL =
            """
            CREATE TABLE IF NOT EXISTS quote_request_lines (
              id BIGINT NOT NULL AUTO_INCREMENT,
              quote_request_id BIGINT NOT NULL,
              product_id BIGINT NOT NULL,
              quantity DECIMAL(14,4) NOT NULL,
              notes VARCHAR(1000) NOT NULL DEFAULT '',
              expected_discount_percent DECIMAL(7,4) NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              CONSTRAINT fk_quote_request_lines_request FOREIGN KEY (quote_request_id) REFERENCES quote_requests (id),
              CONSTRAINT fk_quote_request_lines_product FOREIGN KEY (product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String COMPANY_CUSTOMERS_SQL =
            """
            CREATE TABLE IF NOT EXISTS company_customers (
              id BIGINT NOT NULL AUTO_INCREMENT,
              seller_company_id BIGINT NOT NULL,
              customer_user_id BIGINT NOT NULL,
              customer_tier_id BIGINT NOT NULL,
              seller_customer_id BIGINT NOT NULL,
              status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_company_customers_pair (seller_company_id, customer_user_id),
              CONSTRAINT fk_company_customers_seller FOREIGN KEY (seller_company_id) REFERENCES companies (id),
              CONSTRAINT fk_company_customers_user FOREIGN KEY (customer_user_id) REFERENCES users (id),
              CONSTRAINT fk_company_customers_tier FOREIGN KEY (customer_tier_id) REFERENCES customer_tiers (id),
              CONSTRAINT fk_company_customers_crm FOREIGN KEY (seller_customer_id) REFERENCES customers (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final DataSource dataSource;

    public QuoteRequestDataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute(SEQUENCES_SQL);
            statement.execute(REQUESTS_SQL);
            statement.execute(LINES_SQL);
            statement.execute(COMPANY_CUSTOMERS_SQL);
            addColumnIgnoreDuplicate(
                    statement, "ALTER TABLE quote_requests ADD COLUMN expected_discount_percent DECIMAL(7,4) NULL");
            addColumnIgnoreDuplicate(
                    statement,
                    "ALTER TABLE quote_request_lines ADD COLUMN expected_discount_percent DECIMAL(7,4) NULL");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        log.info("Quote request schema ready.");
    }

    private static void addColumnIgnoreDuplicate(Statement statement, String sql) throws SQLException {
        try {
            statement.execute(sql);
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 1060) {
                throw ex;
            }
        }
    }
}

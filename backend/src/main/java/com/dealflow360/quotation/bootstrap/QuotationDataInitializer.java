package com.dealflow360.quotation.bootstrap;

import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.upsell.repository.UpsellRuleRepository;
import java.math.BigDecimal;
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
@Order(3)
public class QuotationDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuotationDataInitializer.class);

    private static final String CUSTOMERS_SQL =
            """
            CREATE TABLE IF NOT EXISTS customers (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              customer_tier_id BIGINT NOT NULL,
              customer_user_id BIGINT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_customers_company_name (company_id, name),
              UNIQUE KEY uk_customers_company_user (company_id, customer_user_id),
              CONSTRAINT fk_customers_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_customers_tier FOREIGN KEY (customer_tier_id) REFERENCES customer_tiers (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String QUOTATIONS_SQL =
            """
            CREATE TABLE IF NOT EXISTS quotations (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              quote_number VARCHAR(32) NOT NULL,
              customer_id BIGINT NOT NULL,
              sales_rep_id BIGINT NOT NULL,
              price_list_id BIGINT NOT NULL,
              status VARCHAR(32) NOT NULL,
              subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
              discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
              total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
              total_cost DECIMAL(14,2) NOT NULL DEFAULT 0,
              margin_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
              margin_percent DECIMAL(10,4) NOT NULL DEFAULT 0,
              risk_score DECIMAL(10,4) NOT NULL DEFAULT 0,
              risk_level VARCHAR(16) NOT NULL DEFAULT 'NONE',
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              submitted_at TIMESTAMP NULL,
              manager_approved_at TIMESTAMP NULL,
              finance_approved_at TIMESTAMP NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_quotations_company_number (company_id, quote_number),
              CONSTRAINT fk_quotations_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_quotations_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
              CONSTRAINT fk_quotations_sales_rep FOREIGN KEY (sales_rep_id) REFERENCES users (id),
              CONSTRAINT fk_quotations_price_list FOREIGN KEY (price_list_id) REFERENCES price_lists (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String LINES_SQL =
            """
            CREATE TABLE IF NOT EXISTS quotation_lines (
              id BIGINT NOT NULL AUTO_INCREMENT,
              quotation_id BIGINT NOT NULL,
              product_id BIGINT NOT NULL,
              variant_id BIGINT NULL,
              quantity DECIMAL(12,4) NOT NULL,
              base_unit_price DECIMAL(12,2) NOT NULL,
              resolved_unit_price DECIMAL(12,2) NOT NULL,
              cost_price DECIMAL(12,2) NOT NULL,
              discount_percent DECIMAL(7,4) NOT NULL DEFAULT 0,
              discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
              allowed_discount_percent DECIMAL(7,4) NOT NULL DEFAULT 0,
              line_total DECIMAL(14,2) NOT NULL DEFAULT 0,
              margin_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
              margin_percent DECIMAL(10,4) NOT NULL DEFAULT 0,
              billing_type VARCHAR(16) NOT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              CONSTRAINT fk_quotation_lines_quotation FOREIGN KEY (quotation_id) REFERENCES quotations (id),
              CONSTRAINT fk_quotation_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
              CONSTRAINT fk_quotation_lines_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String DISMISSALS_SQL =
            """
            CREATE TABLE IF NOT EXISTS quotation_dismissed_suggestions (
              quotation_id BIGINT NOT NULL,
              product_id BIGINT NOT NULL,
              PRIMARY KEY (quotation_id, product_id),
              CONSTRAINT fk_quote_dismissals_quote FOREIGN KEY (quotation_id) REFERENCES quotations (id),
              CONSTRAINT fk_quote_dismissals_product FOREIGN KEY (product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final DataSource dataSource;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository tierRepository;
    private final ProductRepository productRepository;
    private final UpsellRuleRepository upsellRuleRepository;

    public QuotationDataInitializer(
            DataSource dataSource,
            CompanyRepository companyRepository,
            CustomerRepository customerRepository,
            CustomerTierRepository tierRepository,
            ProductRepository productRepository,
            UpsellRuleRepository upsellRuleRepository) {
        this.dataSource = dataSource;
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
        this.tierRepository = tierRepository;
        this.productRepository = productRepository;
        this.upsellRuleRepository = upsellRuleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seedAcme();
        log.info("Quotation schema ready.");
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

    private void createTables() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CUSTOMERS_SQL);
            statement.execute(QUOTATIONS_SQL);
            statement.execute(LINES_SQL);
            statement.execute(DISMISSALS_SQL);
            addColumnIgnoreDuplicate(statement, "ALTER TABLE quotations ADD COLUMN manager_approved_at TIMESTAMP NULL");
            addColumnIgnoreDuplicate(statement, "ALTER TABLE quotations ADD COLUMN finance_approved_at TIMESTAMP NULL");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void seedAcme() {
        Company acme = companyRepository.findByCode("ACME").orElse(null);
        if (acme == null) {
            return;
        }
        long companyId = acme.id();
        CustomerTier gold = tierRepository.findByCompany(companyId).stream()
                .filter(tier -> "Gold".equalsIgnoreCase(tier.name()))
                .findFirst()
                .orElse(null);
        if (gold != null && customerRepository.findByCompanyAndName(companyId, "Acme Corp").isEmpty()) {
            customerRepository.insert(companyId, "Acme Corp", gold.id(), true);
        }

        Product laptop = namedProduct(companyId, "Laptop");
        Product premiumSupport = namedProduct(companyId, "Premium Support");
        if (laptop == null || premiumSupport == null) {
            return;
        }
        try {
            upsellRuleRepository.insert(
                    companyId,
                    laptop.id(),
                    premiumSupport.id(),
                    new BigDecimal("0.9000"),
                    new BigDecimal("0.1500"),
                    new BigDecimal("20.0000"),
                    true);
        } catch (ConflictException ignored) {
            // Pair already seeded.
        }
    }

    private Product namedProduct(long companyId, String name) {
        return productRepository.findByCompany(companyId).stream()
                .filter(product -> name.equalsIgnoreCase(product.name()))
                .findFirst()
                .orElse(null);
    }
}

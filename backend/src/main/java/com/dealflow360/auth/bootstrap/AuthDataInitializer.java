package com.dealflow360.auth.bootstrap;

import com.dealflow360.auth.model.UserRole;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AuthDataInitializer implements ApplicationRunner {

    /**
     * Shared seed password for demo employees only. Not from the hackathon PDF.
     * Signup still uses the password the user types.
     */
    public static final String DEMO_PASSWORD = "DemoPass123!";

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

    private static final String COMPANIES_SQL =
            """
            CREATE TABLE IF NOT EXISTS companies (
              id BIGINT NOT NULL AUTO_INCREMENT,
              name VARCHAR(255) NOT NULL,
              code VARCHAR(64) NOT NULL,
              description VARCHAR(1024) NOT NULL DEFAULT '',
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_companies_code (code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String USERS_SQL =
            """
            CREATE TABLE IF NOT EXISTS users (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NULL,
              name VARCHAR(255) NOT NULL,
              email VARCHAR(255) NOT NULL,
              password_hash VARCHAR(255) NOT NULL,
              role VARCHAR(32) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_users_email (email),
              CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final DataSource dataSource;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(
            DataSource dataSource,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seed();
        log.info("Auth schema ready. Demo seed password is documented as DEMO_PASSWORD in AuthDataInitializer.");
    }

    private void createTables() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute(COMPANIES_SQL);
            statement.execute(USERS_SQL);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void seed() {
        Company acme = companyRepository
                .findByCode("ACME")
                .orElseGet(
                        () -> companyRepository.insert(
                                "Acme Technologies",
                                "ACME",
                                "Hardware, services, and subscription products for business teams."));
        companyRepository.updateDisplay(
                acme.id(),
                "Acme Technologies",
                "Hardware, services, and subscription products for business teams.");
        seedUser(acme.id(), "Acme Admin", "admin@acme.demo", UserRole.ADMIN, true);
        seedUser(acme.id(), "Acme Sales", "sales@acme.demo", UserRole.SALES_REP, true);
        seedUser(acme.id(), "Acme Manager", "manager@acme.demo", UserRole.SALES_MANAGER, true);
        seedUser(acme.id(), "Acme Finance", "finance@acme.demo", UserRole.FINANCE_OPS, true);
        seedUser(acme.id(), "Acme Inactive", "inactive@acme.demo", UserRole.ADMIN, false);

        Company urban = companyRepository
                .findByCode("URBAN")
                .orElseGet(
                        () -> companyRepository.insert(
                                "Urban Systems",
                                "URBAN",
                                "Workplace furniture and on-site installation."));
        seedUser(urban.id(), "Urban Admin", "admin@urban.demo", UserRole.ADMIN, true);
        seedUser(urban.id(), "Urban Sales", "sales@urban.demo", UserRole.SALES_REP, true);
        seedUser(urban.id(), "Urban Manager", "manager@urban.demo", UserRole.SALES_MANAGER, true);
        seedUser(urban.id(), "Urban Finance", "finance@urban.demo", UserRole.FINANCE_OPS, true);

        Company nova = companyRepository
                .findByCode("NOVA")
                .orElseGet(
                        () -> companyRepository.insert(
                                "Nova Cloud",
                                "NOVA",
                                "Cloud backup and managed software services."));
        seedUser(nova.id(), "Nova Admin", "admin@nova.demo", UserRole.ADMIN, true);
        seedUser(nova.id(), "Nova Sales", "sales@nova.demo", UserRole.SALES_REP, true);
        seedUser(nova.id(), "Nova Manager", "manager@nova.demo", UserRole.SALES_MANAGER, true);
        seedUser(nova.id(), "Nova Finance", "finance@nova.demo", UserRole.FINANCE_OPS, true);

        seedCustomer("Rahul Sharma", "customer@example.com");
    }

    private void seedCustomer(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.insert(null, name, email, passwordEncoder.encode(DEMO_PASSWORD), UserRole.CUSTOMER, true);
    }

    private void seedUser(long companyId, String name, String email, UserRole role, boolean active) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.insert(companyId, name, email, passwordEncoder.encode(DEMO_PASSWORD), role, active);
    }
}
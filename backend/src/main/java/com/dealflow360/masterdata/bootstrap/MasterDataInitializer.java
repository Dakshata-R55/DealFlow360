package com.dealflow360.masterdata.bootstrap;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductCategory;
import com.dealflow360.catalog.repository.ProductCategoryRepository;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.catalog.repository.ProductVariantRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.policy.repository.ApprovalPolicyRepository;
import com.dealflow360.policy.repository.DiscountPolicyRepository;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.pricing.repository.PriceListRepository;
import com.dealflow360.subscription.model.CancellationRule;
import com.dealflow360.subscription.model.PlanCycle;
import com.dealflow360.subscription.model.ProrationRule;
import com.dealflow360.subscription.repository.SubscriptionPlanRepository;
import com.dealflow360.standing.service.StandingService;
import com.dealflow360.upsell.repository.UpsellRuleRepository;
import com.dealflow360.warehouse.model.Warehouse;
import com.dealflow360.warehouse.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class MasterDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterDataInitializer.class);
    private static final BigDecimal MANAGER_LINE_EXCESS = new BigDecimal("1.0000");
    private static final BigDecimal FINANCE_LINE_EXCESS = new BigDecimal("8.0000");
    private static final BigDecimal MANAGER_QUOTE_EXCESS = new BigDecimal("0.5000");
    private static final BigDecimal FINANCE_QUOTE_EXCESS = new BigDecimal("2.0000");

    private static final String CATEGORIES_SQL =
            """
            CREATE TABLE IF NOT EXISTS product_categories (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_product_categories_company_name (company_id, name),
              CONSTRAINT fk_product_categories_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String PRODUCTS_SQL =
            """
            CREATE TABLE IF NOT EXISTS products (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              category_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              description VARCHAR(1024) NOT NULL DEFAULT '',
              unit VARCHAR(32) NOT NULL,
              base_price DECIMAL(12,2) NOT NULL,
              cost_price DECIMAL(12,2) NOT NULL,
              tax_percent DECIMAL(7,4) NOT NULL,
              billing_type VARCHAR(16) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_products_company_name (company_id, name),
              CONSTRAINT fk_products_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String VARIANTS_SQL =
            """
            CREATE TABLE IF NOT EXISTS product_variants (
              id BIGINT NOT NULL AUTO_INCREMENT,
              product_id BIGINT NOT NULL,
              attribute_name VARCHAR(64) NOT NULL,
              attribute_value VARCHAR(64) NOT NULL,
              extra_price DECIMAL(12,2) NOT NULL DEFAULT 0,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String TIERS_SQL =
            """
            CREATE TABLE IF NOT EXISTS customer_tiers (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(64) NOT NULL,
              default_discount_limit DECIMAL(7,4) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_customer_tiers_company_name (company_id, name),
              CONSTRAINT fk_customer_tiers_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String PRICE_LISTS_SQL =
            """
            CREATE TABLE IF NOT EXISTS price_lists (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              currency VARCHAR(8) NOT NULL,
              customer_tier_id BIGINT NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_price_lists_company_name (company_id, name),
              CONSTRAINT fk_price_lists_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_price_lists_tier FOREIGN KEY (customer_tier_id) REFERENCES customer_tiers (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String PRICE_LIST_ITEMS_SQL =
            """
            CREATE TABLE IF NOT EXISTS price_list_items (
              price_list_id BIGINT NOT NULL,
              product_id BIGINT NOT NULL,
              price DECIMAL(12,2) NOT NULL,
              PRIMARY KEY (price_list_id, product_id),
              CONSTRAINT fk_price_list_items_list FOREIGN KEY (price_list_id) REFERENCES price_lists (id),
              CONSTRAINT fk_price_list_items_product FOREIGN KEY (product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String DISCOUNT_POLICIES_SQL =
            """
            CREATE TABLE IF NOT EXISTS discount_policies (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              customer_tier_id BIGINT NULL,
              category_id BIGINT NULL,
              max_discount_pct DECIMAL(7,4) NOT NULL,
              PRIMARY KEY (id),
              CONSTRAINT fk_discount_policies_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_discount_policies_tier FOREIGN KEY (customer_tier_id) REFERENCES customer_tiers (id),
              CONSTRAINT fk_discount_policies_category FOREIGN KEY (category_id) REFERENCES product_categories (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String APPROVAL_POLICIES_SQL =
            """
            CREATE TABLE IF NOT EXISTS approval_policies (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              manager_line_excess_pct DECIMAL(10,4) NOT NULL,
              finance_line_excess_pct DECIMAL(10,4) NOT NULL,
              manager_quote_excess_pct DECIMAL(10,4) NOT NULL,
              finance_quote_excess_pct DECIMAL(10,4) NOT NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_approval_policies_company (company_id),
              CONSTRAINT fk_approval_policies_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String WAREHOUSES_SQL =
            """
            CREATE TABLE IF NOT EXISTS warehouses (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              location VARCHAR(255) NOT NULL,
              shipping_cost_weight DECIMAL(10,4) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_warehouses_company_name (company_id, name),
              CONSTRAINT fk_warehouses_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String INVENTORY_SQL =
            """
            CREATE TABLE IF NOT EXISTS warehouse_inventory (
              warehouse_id BIGINT NOT NULL,
              product_id BIGINT NOT NULL,
              on_hand INT NOT NULL DEFAULT 0,
              reserved INT NOT NULL DEFAULT 0,
              min_stock INT NOT NULL DEFAULT 0,
              reorder_qty INT NOT NULL DEFAULT 0,
              PRIMARY KEY (warehouse_id, product_id),
              CONSTRAINT fk_warehouse_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
              CONSTRAINT fk_warehouse_inventory_product FOREIGN KEY (product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String PLANS_SQL =
            """
            CREATE TABLE IF NOT EXISTS subscription_plans (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              name VARCHAR(255) NOT NULL,
              cycle VARCHAR(16) NOT NULL,
              proration_rule VARCHAR(64) NOT NULL,
              cancellation_rule VARCHAR(64) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_subscription_plans_company_name (company_id, name),
              CONSTRAINT fk_subscription_plans_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String UPSELL_SQL =
            """
            CREATE TABLE IF NOT EXISTS upsell_rules (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              trigger_product_id BIGINT NOT NULL,
              suggested_product_id BIGINT NOT NULL,
              score DECIMAL(10,4) NOT NULL,
              promotion_boost DECIMAL(10,4) NOT NULL,
              min_margin_pct DECIMAL(7,4) NOT NULL,
              active TINYINT(1) NOT NULL DEFAULT 1,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_upsell_rules_pair (company_id, trigger_product_id, suggested_product_id),
              CONSTRAINT fk_upsell_rules_company FOREIGN KEY (company_id) REFERENCES companies (id),
              CONSTRAINT fk_upsell_rules_trigger FOREIGN KEY (trigger_product_id) REFERENCES products (id),
              CONSTRAINT fk_upsell_rules_suggested FOREIGN KEY (suggested_product_id) REFERENCES products (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String STANDING_RULES_SQL =
            """
            CREATE TABLE IF NOT EXISTS standing_rules (
              id BIGINT NOT NULL AUTO_INCREMENT,
              company_id BIGINT NOT NULL,
              silver_min_spend DECIMAL(12,2) NOT NULL,
              gold_min_spend DECIMAL(12,2) NOT NULL,
              window_months INT NOT NULL DEFAULT 6,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_standing_rules_company (company_id),
              CONSTRAINT fk_standing_rules_company FOREIGN KEY (company_id) REFERENCES companies (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final DataSource dataSource;
    private final CompanyRepository companyRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerTierRepository tierRepository;
    private final PriceListRepository priceListRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;
    private final WarehouseRepository warehouseRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UpsellRuleRepository upsellRuleRepository;
    private final StandingService standingService;

    public MasterDataInitializer(
            DataSource dataSource,
            CompanyRepository companyRepository,
            ProductCategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CustomerTierRepository tierRepository,
            PriceListRepository priceListRepository,
            DiscountPolicyRepository discountPolicyRepository,
            ApprovalPolicyRepository approvalPolicyRepository,
            WarehouseRepository warehouseRepository,
            SubscriptionPlanRepository planRepository,
            UpsellRuleRepository upsellRuleRepository,
            StandingService standingService) {
        this.dataSource = dataSource;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.tierRepository = tierRepository;
        this.priceListRepository = priceListRepository;
        this.discountPolicyRepository = discountPolicyRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
        this.warehouseRepository = warehouseRepository;
        this.planRepository = planRepository;
        this.upsellRuleRepository = upsellRuleRepository;
        this.standingService = standingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seedAcme();
        seedSellerCatalog("URBAN", urbanProducts());
        seedSellerCatalog("NOVA", novaProducts());
        for (Company company : companyRepository.findActive("")) {
            seedApprovalIfMissing(company.id());
            standingService.ensureDefault(company.id());
        }
        log.info("Master data schema ready.");
    }

    private void createTables() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CATEGORIES_SQL);
            statement.execute(PRODUCTS_SQL);
            statement.execute(VARIANTS_SQL);
            statement.execute(TIERS_SQL);
            statement.execute(PRICE_LISTS_SQL);
            statement.execute(PRICE_LIST_ITEMS_SQL);
            statement.execute(DISCOUNT_POLICIES_SQL);
            migrateApprovalPolicies(connection, statement);
            statement.execute(WAREHOUSES_SQL);
            statement.execute(INVENTORY_SQL);
            statement.execute(PLANS_SQL);
            statement.execute(UPSELL_SQL);
            statement.execute(STANDING_RULES_SQL);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void seedAcme() {
        Company acme = companyRepository.findByCode("ACME").orElse(null);
        if (acme == null || categoryRepository.existsForCompany(acme.id())) {
            return;
        }
        long companyId = acme.id();

        ProductCategory hardware = categoryRepository.insert(companyId, "Hardware", true);
        ProductCategory services = categoryRepository.insert(companyId, "Services", true);
        ProductCategory subscriptions = categoryRepository.insert(companyId, "Subscriptions", true);

        CustomerTier bronze = tierRepository.insert(companyId, "Bronze", new BigDecimal("5.0000"), true);
        CustomerTier silver = tierRepository.insert(companyId, "Silver", new BigDecimal("10.0000"), true);
        CustomerTier gold = tierRepository.insert(companyId, "Gold", new BigDecimal("15.0000"), true);

        Product laptop = productRepository.insert(
                companyId,
                hardware.id(),
                "Laptop",
                "Standard business laptop",
                "piece",
                new BigDecimal("120000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("18.0000"),
                BillingType.ONE_TIME,
                true);
        Product setup = productRepository.insert(
                companyId,
                services.id(),
                "Setup Service",
                "Onsite setup and configuration",
                "hour",
                new BigDecimal("8000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("18.0000"),
                BillingType.ONE_TIME,
                true);
        productRepository.insert(
                companyId,
                subscriptions.id(),
                "Premium Support",
                "Recurring premium support plan",
                "plan",
                new BigDecimal("15000.00"),
                new BigDecimal("4000.00"),
                new BigDecimal("18.0000"),
                BillingType.RECURRING,
                true);

        variantRepository.insert(companyId, laptop.id(), "Size", "16GB", new BigDecimal("8000.00"));

        PriceList goldList = priceListRepository.insert(companyId, "Gold INR", "INR", gold.id(), true);
        priceListRepository.insert(companyId, "Bronze INR", "INR", bronze.id(), true);
        priceListRepository.insert(companyId, "Silver INR", "INR", silver.id(), true);
        priceListRepository.upsertItem(goldList.id(), laptop.id(), new BigDecimal("110000.00"));

        discountPolicyRepository.insert(companyId, bronze.id(), null, new BigDecimal("5.0000"));
        discountPolicyRepository.insert(companyId, silver.id(), null, new BigDecimal("10.0000"));
        discountPolicyRepository.insert(companyId, gold.id(), null, new BigDecimal("15.0000"));
        discountPolicyRepository.insert(companyId, null, hardware.id(), new BigDecimal("15.0000"));
        discountPolicyRepository.insert(companyId, null, services.id(), new BigDecimal("10.0000"));
        discountPolicyRepository.insert(companyId, null, subscriptions.id(), new BigDecimal("8.0000"));

        seedApprovalIfMissing(companyId);

        Warehouse bangalore =
                warehouseRepository.insert(companyId, "Bangalore", "Bengaluru", new BigDecimal("1.0000"), true);
        Warehouse pune = warehouseRepository.insert(companyId, "Pune", "Pune", new BigDecimal("1.2000"), true);
        warehouseRepository.upsertInventory(bangalore.id(), laptop.id(), 12, 0, 2, 5);
        warehouseRepository.upsertInventory(pune.id(), laptop.id(), 8, 0, 2, 5);

        planRepository.insert(companyId, "Monthly Support", PlanCycle.MONTHLY, ProrationRule.PRORATE_DAYS, CancellationRule.CREDIT_NOTE, true);
        planRepository.insert(companyId, "Annual Support", PlanCycle.YEARLY, ProrationRule.PRORATE_DAYS, CancellationRule.CREDIT_NOTE, true);

        upsellRuleRepository.insert(
                companyId,
                laptop.id(),
                setup.id(),
                new BigDecimal("0.8000"),
                new BigDecimal("0.2000"),
                new BigDecimal("20.0000"),
                true);
    }

    private void seedSellerCatalog(String companyCode, List<SeedProduct> products) {
        Company company = companyRepository.findByCode(companyCode).orElse(null);
        if (company == null || categoryRepository.existsForCompany(company.id())) {
            return;
        }
        long companyId = company.id();
        ProductCategory hardware = categoryRepository.insert(companyId, "Hardware", true);
        ProductCategory services = categoryRepository.insert(companyId, "Services", true);
        ProductCategory subscriptions = categoryRepository.insert(companyId, "Subscriptions", true);

        CustomerTier bronze = tierRepository.insert(companyId, "Bronze", new BigDecimal("5.0000"), true);
        CustomerTier silver = tierRepository.insert(companyId, "Silver", new BigDecimal("10.0000"), true);
        CustomerTier gold = tierRepository.insert(companyId, "Gold", new BigDecimal("15.0000"), true);
        priceListRepository.insert(companyId, "Bronze INR", "INR", bronze.id(), true);
        priceListRepository.insert(companyId, "Silver INR", "INR", silver.id(), true);
        priceListRepository.insert(companyId, "Gold INR", "INR", gold.id(), true);

        for (SeedProduct seed : products) {
            long categoryId =
                    "Hardware".equals(seed.category) ? hardware.id() : "Services".equals(seed.category) ? services.id() : subscriptions.id();
            productRepository.insert(
                    companyId,
                    categoryId,
                    seed.name,
                    seed.description,
                    seed.unit,
                    seed.basePrice,
                    seed.costPrice,
                    new BigDecimal("18.0000"),
                    seed.billingType,
                    true);
        }

        discountPolicyRepository.insert(companyId, bronze.id(), null, new BigDecimal("5.0000"));
        discountPolicyRepository.insert(companyId, silver.id(), null, new BigDecimal("10.0000"));
        discountPolicyRepository.insert(companyId, gold.id(), null, new BigDecimal("15.0000"));
        discountPolicyRepository.insert(companyId, null, hardware.id(), new BigDecimal("15.0000"));
        discountPolicyRepository.insert(companyId, null, services.id(), new BigDecimal("10.0000"));
        discountPolicyRepository.insert(companyId, null, subscriptions.id(), new BigDecimal("8.0000"));

        seedApprovalIfMissing(companyId);
        warehouseRepository.insert(companyId, "Main", company.name(), new BigDecimal("1.0000"), true);
    }

    private static List<SeedProduct> urbanProducts() {
        return List.of(
                new SeedProduct(
                        "Desk",
                        "Hardware",
                        "Standing office desk",
                        "piece",
                        new BigDecimal("45000.00"),
                        new BigDecimal("22000.00"),
                        BillingType.ONE_TIME),
                new SeedProduct(
                        "Chair",
                        "Hardware",
                        "Ergonomic task chair",
                        "piece",
                        new BigDecimal("18000.00"),
                        new BigDecimal("8000.00"),
                        BillingType.ONE_TIME),
                new SeedProduct(
                        "Installation",
                        "Services",
                        "On-site furniture installation",
                        "job",
                        new BigDecimal("6000.00"),
                        new BigDecimal("2500.00"),
                        BillingType.ONE_TIME),
                new SeedProduct(
                        "Workplace Care",
                        "Subscriptions",
                        "Monthly furniture maintenance",
                        "plan",
                        new BigDecimal("4000.00"),
                        new BigDecimal("1200.00"),
                        BillingType.RECURRING));
    }

    private static List<SeedProduct> novaProducts() {
        return List.of(
                new SeedProduct(
                        "Cloud Backup",
                        "Subscriptions",
                        "Encrypted backup for business files",
                        "plan",
                        new BigDecimal("9000.00"),
                        new BigDecimal("2500.00"),
                        BillingType.RECURRING),
                new SeedProduct(
                        "Managed Firewall",
                        "Subscriptions",
                        "Managed network protection",
                        "plan",
                        new BigDecimal("12000.00"),
                        new BigDecimal("4000.00"),
                        BillingType.RECURRING),
                new SeedProduct(
                        "Migration Service",
                        "Services",
                        "One-time cloud migration",
                        "job",
                        new BigDecimal("35000.00"),
                        new BigDecimal("15000.00"),
                        BillingType.ONE_TIME),
                new SeedProduct(
                        "Access Point",
                        "Hardware",
                        "Office wireless access point",
                        "piece",
                        new BigDecimal("14000.00"),
                        new BigDecimal("7000.00"),
                        BillingType.ONE_TIME));
    }

    private void migrateApprovalPolicies(Connection connection, Statement statement) throws SQLException {
        boolean needsRebuild = false;
        try (Statement probe = connection.createStatement()) {
            probe.executeQuery("SELECT manager_line_excess_pct FROM approval_policies LIMIT 1");
        } catch (SQLException ignored) {
            needsRebuild = true;
        }
        if (needsRebuild) {
            statement.execute("DROP TABLE IF EXISTS approval_policies");
        }
        statement.execute(APPROVAL_POLICIES_SQL);
    }

    private void seedApprovalIfMissing(long companyId) {
        if (approvalPolicyRepository.findByCompany(companyId).isPresent()) {
            return;
        }
        approvalPolicyRepository.insert(
                companyId, MANAGER_LINE_EXCESS, FINANCE_LINE_EXCESS, MANAGER_QUOTE_EXCESS, FINANCE_QUOTE_EXCESS);
    }

    private record SeedProduct(
            String name,
            String category,
            String description,
            String unit,
            BigDecimal basePrice,
            BigDecimal costPrice,
            BillingType billingType) {}
}

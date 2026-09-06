package com.dealflow360.standing.service;

import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.quoterequest.model.CompanyCustomer;
import com.dealflow360.quoterequest.repository.CompanyCustomerRepository;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.standing.dto.StandingProgressResponse;
import com.dealflow360.standing.dto.StandingRuleRequest;
import com.dealflow360.standing.dto.StandingRuleResponse;
import com.dealflow360.standing.model.StandingRule;
import com.dealflow360.standing.repository.StandingRuleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StandingService {

    private static final BigDecimal DEFAULT_SILVER = new BigDecimal("50000.00");
    private static final BigDecimal DEFAULT_GOLD = new BigDecimal("200000.00");
    private static final int DEFAULT_WINDOW = 6;

    private final StandingRuleRepository standingRuleRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository tierRepository;
    private final CompanyCustomerRepository companyCustomerRepository;
    private final CompanyRepository companyRepository;

    public StandingService(
            StandingRuleRepository standingRuleRepository,
            CustomerRepository customerRepository,
            CustomerTierRepository tierRepository,
            CompanyCustomerRepository companyCustomerRepository,
            CompanyRepository companyRepository) {
        this.standingRuleRepository = standingRuleRepository;
        this.customerRepository = customerRepository;
        this.tierRepository = tierRepository;
        this.companyCustomerRepository = companyCustomerRepository;
        this.companyRepository = companyRepository;
    }

    public StandingRuleResponse getRule(long companyId) {
        return StandingRuleResponse.from(requireRule(companyId));
    }

    @Transactional
    public StandingRuleResponse saveRule(long companyId, StandingRuleRequest request) {
        int window = request.windowMonths() == null ? DEFAULT_WINDOW : request.windowMonths();
        return StandingRuleResponse.from(
                standingRuleRepository.upsert(companyId, request.silverMinSpend(), request.goldMinSpend(), window));
    }

    @Transactional
    public void evaluateAfterConfirm(long companyId, long customerId) {
        Customer customer = customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        StandingRule rule = requireRule(companyId);
        BigDecimal spend = standingRuleRepository.confirmedSpend(companyId, customerId, rule.windowMonths());
        String target = targetName(spend, rule);
        if (target == null) {
            return;
        }
        CustomerTier current = tierRepository
                .findById(customer.customerTierId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
        if (rank(target) <= rank(current.name())) {
            return;
        }
        CustomerTier next = findTierByName(companyId, target);
        if (next == null) {
            return;
        }
        applyTier(companyId, customerId, next.id());
    }

    @Transactional
    public Customer applyTier(long companyId, long customerId, long customerTierId) {
        customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        tierRepository
                .findById(customerTierId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
        customerRepository.updateTier(customerId, companyId, customerTierId);
        companyCustomerRepository.updateTierBySellerCustomer(companyId, customerId, customerTierId);
        return customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    public List<StandingProgressResponse> progressForCustomer(long customerUserId) {
        List<StandingProgressResponse> rows = new ArrayList<>();
        for (CompanyCustomer link : companyCustomerRepository.findByCustomerUser(customerUserId)) {
            rows.add(progress(link.sellerCompanyId(), link.sellerCustomerId()));
        }
        return rows;
    }

    public StandingRule ensureDefault(long companyId) {
        return standingRuleRepository
                .findByCompany(companyId)
                .orElseGet(() -> standingRuleRepository.insert(companyId, DEFAULT_SILVER, DEFAULT_GOLD, DEFAULT_WINDOW));
    }

    private StandingProgressResponse progress(long companyId, long customerId) {
        Customer customer = customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerTier current = tierRepository
                .findById(customer.customerTierId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
        StandingRule rule = requireRule(companyId);
        BigDecimal spend = standingRuleRepository.confirmedSpend(companyId, customerId, rule.windowMonths());
        String seller = companyRepository.findById(companyId).map(company -> company.name()).orElse("");
        String next = nextAbove(current.name(), rule);
        BigDecimal remaining = null;
        if ("Silver".equalsIgnoreCase(next)) {
            remaining = rule.silverMinSpend().subtract(spend).max(BigDecimal.ZERO);
        } else if ("Gold".equalsIgnoreCase(next)) {
            remaining = rule.goldMinSpend().subtract(spend).max(BigDecimal.ZERO);
        }
        return new StandingProgressResponse(
                companyId,
                seller,
                current.name(),
                spend,
                rule.windowMonths(),
                rule.silverMinSpend(),
                rule.goldMinSpend(),
                next,
                remaining);
    }

    private StandingRule requireRule(long companyId) {
        return standingRuleRepository.findByCompany(companyId).orElseGet(() -> ensureDefault(companyId));
    }

    private CustomerTier findTierByName(long companyId, String name) {
        return tierRepository.findByCompany(companyId).stream()
                .filter(tier -> name.equalsIgnoreCase(tier.name()))
                .findFirst()
                .orElse(null);
    }

    private static String targetName(BigDecimal spend, StandingRule rule) {
        if (rule.goldMinSpend().compareTo(BigDecimal.ZERO) > 0 && spend.compareTo(rule.goldMinSpend()) >= 0) {
            return "Gold";
        }
        if (rule.silverMinSpend().compareTo(BigDecimal.ZERO) > 0 && spend.compareTo(rule.silverMinSpend()) >= 0) {
            return "Silver";
        }
        return null;
    }

    private static String nextAbove(String current, StandingRule rule) {
        int currentRank = rank(current);
        if (currentRank < rank("Silver")
                && rule.silverMinSpend().compareTo(BigDecimal.ZERO) > 0) {
            return "Silver";
        }
        if (currentRank < rank("Gold") && rule.goldMinSpend().compareTo(BigDecimal.ZERO) > 0) {
            return "Gold";
        }
        return null;
    }

    private static int rank(String name) {
        if (name == null) {
            return 0;
        }
        String key = name.trim().toLowerCase();
        if (key.equals("gold")) {
            return 2;
        }
        if (key.equals("silver")) {
            return 1;
        }
        return 0;
    }
}

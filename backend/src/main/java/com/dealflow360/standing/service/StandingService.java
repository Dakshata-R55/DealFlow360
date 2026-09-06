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
import java.util.Comparator;
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

    public List<StandingRuleResponse> listRules(long companyId) {
        ensureDefault(companyId);
        return standingRuleRepository.findByCompany(companyId).stream()
                .map(StandingRuleResponse::from)
                .toList();
    }

    @Transactional
    public StandingRuleResponse saveRule(long companyId, long customerTierId, StandingRuleRequest request) {
        tierRepository
                .findById(customerTierId, companyId)
                .orElseThrow(() -> new NotFoundException("Standing not found"));
        int window = request.windowMonths() == null ? DEFAULT_WINDOW : request.windowMonths();
        return StandingRuleResponse.from(
                standingRuleRepository.upsert(companyId, customerTierId, request.minSpend(), window));
    }

    @Transactional
    public void evaluateAfterConfirm(long companyId, long customerId) {
        Customer customer = customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        ensureDefault(companyId);
        List<StandingRule> rules = standingRuleRepository.findByCompany(companyId);
        StandingRule current = ruleForTier(rules, customer.customerTierId());
        BigDecimal currentMin = current == null ? BigDecimal.ZERO : current.minSpend();
        StandingRule best = null;
        for (StandingRule rule : rules.stream()
                .sorted(Comparator.comparing(StandingRule::minSpend).reversed())
                .toList()) {
            if (rule.minSpend().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal spend = standingRuleRepository.confirmedSpend(companyId, customerId, rule.windowMonths());
            if (spend.compareTo(rule.minSpend()) >= 0) {
                best = rule;
                break;
            }
        }
        if (best == null || best.minSpend().compareTo(currentMin) <= 0) {
            return;
        }
        applyTier(companyId, customerId, best.customerTierId());
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

    public void ensureDefault(long companyId) {
        for (CustomerTier tier : tierRepository.findByCompany(companyId)) {
            ensureForTier(companyId, tier);
        }
    }

    public void ensureForTier(long companyId, CustomerTier tier) {
        if (standingRuleRepository.findByTier(companyId, tier.id()).isPresent()) {
            return;
        }
        standingRuleRepository.insert(companyId, tier.id(), defaultMinSpend(tier.name()), DEFAULT_WINDOW);
    }

    private StandingProgressResponse progress(long companyId, long customerId) {
        Customer customer = customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerTier current = tierRepository
                .findById(customer.customerTierId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
        ensureDefault(companyId);
        List<StandingRule> rules = standingRuleRepository.findByCompany(companyId);
        StandingRule currentRule = ruleForTier(rules, current.id());
        int window = currentRule == null ? DEFAULT_WINDOW : currentRule.windowMonths();
        BigDecimal spend = standingRuleRepository.confirmedSpend(companyId, customerId, window);
        String seller = companyRepository.findById(companyId).map(company -> company.name()).orElse("");
        BigDecimal currentMin = currentRule == null ? BigDecimal.ZERO : currentRule.minSpend();
        StandingRule next = rules.stream()
                .filter(rule -> rule.minSpend().compareTo(currentMin) > 0)
                .min(Comparator.comparing(StandingRule::minSpend))
                .orElse(null);
        String nextName = null;
        BigDecimal remaining = null;
        if (next != null) {
            CustomerTier nextTier = tierRepository.findById(next.customerTierId(), companyId).orElse(null);
            nextName = nextTier == null ? null : nextTier.name();
            BigDecimal nextSpend = standingRuleRepository.confirmedSpend(companyId, customerId, next.windowMonths());
            remaining = next.minSpend().subtract(nextSpend).max(BigDecimal.ZERO);
        }
        return new StandingProgressResponse(
                companyId, seller, current.name(), spend, window, nextName, remaining);
    }

    private static StandingRule ruleForTier(List<StandingRule> rules, long customerTierId) {
        return rules.stream().filter(rule -> rule.customerTierId() == customerTierId).findFirst().orElse(null);
    }

    private static BigDecimal defaultMinSpend(String name) {
        if (name == null) {
            return BigDecimal.ZERO;
        }
        String key = name.trim().toLowerCase();
        if (key.equals("gold")) {
            return DEFAULT_GOLD;
        }
        if (key.equals("silver")) {
            return DEFAULT_SILVER;
        }
        return BigDecimal.ZERO;
    }
}

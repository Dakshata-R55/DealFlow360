package com.dealflow360.quotation.service;

import com.dealflow360.policy.model.ApprovalPolicy;
import com.dealflow360.policy.model.DiscountPolicy;
import com.dealflow360.policy.model.RiskLevel;
import com.dealflow360.policy.repository.ApprovalPolicyRepository;
import com.dealflow360.policy.repository.DiscountPolicyRepository;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.quotation.dto.QuotationResponse.LikelyRoute;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RiskEngine {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_MANAGER_LINE = new BigDecimal("1.0000");
    private static final BigDecimal DEFAULT_FINANCE_LINE = new BigDecimal("8.0000");
    private static final BigDecimal DEFAULT_MANAGER_QUOTE = new BigDecimal("0.5000");
    private static final BigDecimal DEFAULT_FINANCE_QUOTE = new BigDecimal("2.0000");
    private static final int PCT_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public record LineRiskInput(long lineId, long categoryId, BigDecimal baseValue, BigDecimal enteredDiscount) {}

    public record LineRiskResult(long lineId, BigDecimal allowedDiscount, BigDecimal excess) {}

    public record RiskEvaluation(
            BigDecimal quoteExcessPct,
            BigDecimal maxLineExcess,
            RiskLevel level,
            LikelyRoute likelyRoute,
            List<LineRiskResult> lines) {}

    private final DiscountPolicyRepository discountPolicyRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;

    public RiskEngine(
            DiscountPolicyRepository discountPolicyRepository, ApprovalPolicyRepository approvalPolicyRepository) {
        this.discountPolicyRepository = discountPolicyRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
    }

    public BigDecimal standingDiscount(CustomerTier tier, List<DiscountPolicy> policies) {
        for (DiscountPolicy policy : policies) {
            if (policy.customerTierId() != null
                    && policy.categoryId() == null
                    && policy.customerTierId() == tier.id()) {
                return policy.maxDiscountPct();
            }
        }
        return tier.defaultDiscountLimit();
    }

    public BigDecimal categoryDiscount(long categoryId, List<DiscountPolicy> policies) {
        for (DiscountPolicy policy : policies) {
            if (policy.categoryId() != null
                    && policy.customerTierId() == null
                    && policy.categoryId() == categoryId) {
                return policy.maxDiscountPct();
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal allowedDiscount(
            long companyId, CustomerTier tier, long categoryId, List<DiscountPolicy> policies) {
        BigDecimal tierLimit = null;
        BigDecimal categoryLimit = null;
        for (DiscountPolicy policy : policies) {
            if (policy.customerTierId() != null
                    && policy.categoryId() == null
                    && policy.customerTierId() == tier.id()) {
                tierLimit = policy.maxDiscountPct();
            }
            if (policy.categoryId() != null
                    && policy.customerTierId() == null
                    && policy.categoryId() == categoryId) {
                categoryLimit = policy.maxDiscountPct();
            }
        }
        if (tierLimit == null && categoryLimit == null) {
            return tier.defaultDiscountLimit();
        }
        if (tierLimit == null) {
            return categoryLimit;
        }
        if (categoryLimit == null) {
            return tierLimit;
        }
        return tierLimit.min(categoryLimit);
    }

    public RiskEvaluation evaluate(long companyId, CustomerTier tier, List<LineRiskInput> inputs) {
        List<DiscountPolicy> policies = discountPolicyRepository.findByCompany(companyId);
        ApprovalPolicy approval = approvalPolicyRepository.findByCompany(companyId).orElse(defaults());
        List<LineRiskResult> lineResults = new ArrayList<>();
        BigDecimal extraMoney = BigDecimal.ZERO;
        BigDecimal grossSum = BigDecimal.ZERO;
        BigDecimal maxLineExcess = BigDecimal.ZERO;
        for (LineRiskInput input : inputs) {
            BigDecimal allowed = allowedDiscount(companyId, tier, input.categoryId(), policies);
            BigDecimal excess = input.enteredDiscount().subtract(allowed).max(BigDecimal.ZERO);
            lineResults.add(new LineRiskResult(input.lineId(), allowed, excess));
            extraMoney = extraMoney.add(input.baseValue().multiply(excess).divide(HUNDRED, 6, ROUNDING));
            grossSum = grossSum.add(input.baseValue());
            if (excess.compareTo(maxLineExcess) > 0) {
                maxLineExcess = excess;
            }
        }
        BigDecimal quoteExcessPct = grossSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : extraMoney.divide(grossSum, PCT_SCALE + 2, ROUNDING).multiply(HUNDRED).setScale(PCT_SCALE, ROUNDING);
        maxLineExcess = maxLineExcess.setScale(PCT_SCALE, ROUNDING);
        LikelyRoute route = routeFor(maxLineExcess, quoteExcessPct, approval);
        return new RiskEvaluation(quoteExcessPct, maxLineExcess, levelFor(route), route, lineResults);
    }

    public LikelyRoute routeFor(RiskLevel level) {
        return switch (level) {
            case NONE -> new LikelyRoute(false, false);
            case MEDIUM -> new LikelyRoute(true, false);
            case HIGH -> new LikelyRoute(false, true);
        };
    }

    public List<DiscountPolicy> policies(long companyId) {
        return discountPolicyRepository.findByCompany(companyId);
    }

    public Map<Long, LineRiskResult> resultsByLineId(RiskEvaluation evaluation) {
        Map<Long, LineRiskResult> map = new HashMap<>();
        for (LineRiskResult result : evaluation.lines()) {
            map.put(result.lineId(), result);
        }
        return map;
    }

    private static LikelyRoute routeFor(BigDecimal maxLineExcess, BigDecimal quoteExcessPct, ApprovalPolicy approval) {
        if (maxLineExcess.compareTo(approval.financeLineExcessPercent()) >= 0
                || quoteExcessPct.compareTo(approval.financeQuoteExcessPercent()) >= 0) {
            return new LikelyRoute(false, true);
        }
        if (maxLineExcess.compareTo(approval.managerLineExcessPercent()) >= 0
                || quoteExcessPct.compareTo(approval.managerQuoteExcessPercent()) >= 0) {
            return new LikelyRoute(true, false);
        }
        return new LikelyRoute(false, false);
    }

    private static RiskLevel levelFor(LikelyRoute route) {
        if (route.requiresFinance()) {
            return RiskLevel.HIGH;
        }
        if (route.requiresManager()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.NONE;
    }

    private static ApprovalPolicy defaults() {
        return new ApprovalPolicy(
                0L, 0L, DEFAULT_MANAGER_LINE, DEFAULT_FINANCE_LINE, DEFAULT_MANAGER_QUOTE, DEFAULT_FINANCE_QUOTE);
    }
}

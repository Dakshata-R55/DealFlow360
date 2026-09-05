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

    private static final BigDecimal SCORE_HIGH = new BigDecimal("5");
    private static final BigDecimal DEFAULT_HARD_EXCESS = new BigDecimal("8");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("0.60");
    private static final BigDecimal WEIGHTED_WEIGHT = new BigDecimal("0.40");
    private static final int SCORE_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public record LineRiskInput(long lineId, long categoryId, BigDecimal baseValue, BigDecimal enteredDiscount) {}

    public record LineRiskResult(long lineId, BigDecimal allowedDiscount, BigDecimal excess) {}

    public record RiskEvaluation(
            BigDecimal score, RiskLevel level, LikelyRoute likelyRoute, List<LineRiskResult> lines) {}

    private final DiscountPolicyRepository discountPolicyRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;

    public RiskEngine(
            DiscountPolicyRepository discountPolicyRepository, ApprovalPolicyRepository approvalPolicyRepository) {
        this.discountPolicyRepository = discountPolicyRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
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
        List<LineRiskResult> lineResults = new ArrayList<>();
        BigDecimal maxExcess = BigDecimal.ZERO;
        BigDecimal weightedNumerator = BigDecimal.ZERO;
        BigDecimal weightedDenominator = BigDecimal.ZERO;
        for (LineRiskInput input : inputs) {
            BigDecimal allowed = allowedDiscount(companyId, tier, input.categoryId(), policies);
            BigDecimal excess = input.enteredDiscount().subtract(allowed).max(BigDecimal.ZERO);
            lineResults.add(new LineRiskResult(input.lineId(), allowed, excess));
            if (excess.compareTo(maxExcess) > 0) {
                maxExcess = excess;
            }
            weightedNumerator = weightedNumerator.add(input.baseValue().multiply(excess));
            weightedDenominator = weightedDenominator.add(input.baseValue());
        }
        BigDecimal weightedExcess = weightedDenominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedNumerator.divide(weightedDenominator, SCORE_SCALE + 2, ROUNDING);
        BigDecimal score = MAX_WEIGHT.multiply(maxExcess)
                .add(WEIGHTED_WEIGHT.multiply(weightedExcess))
                .setScale(SCORE_SCALE, ROUNDING);
        BigDecimal hardThreshold = hardExcessThreshold(companyId);
        RiskLevel level = classify(score, maxExcess, hardThreshold);
        return new RiskEvaluation(score, level, routeFor(companyId, level), lineResults);
    }

    public LikelyRoute routeFor(long companyId, RiskLevel level) {
        return approvalPolicyRepository.findByCompany(companyId).stream()
                .filter(policy -> policy.riskLevel() == level)
                .findFirst()
                .map(policy -> new LikelyRoute(policy.requiresManager(), policy.requiresFinance()))
                .orElseGet(() -> fallbackRoute(level));
    }

    public List<DiscountPolicy> policies(long companyId) {
        return discountPolicyRepository.findByCompany(companyId);
    }

    private BigDecimal hardExcessThreshold(long companyId) {
        return approvalPolicyRepository.findByCompany(companyId).stream()
                .filter(policy -> policy.riskLevel() == RiskLevel.HIGH)
                .map(ApprovalPolicy::hardLineExcessThreshold)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(DEFAULT_HARD_EXCESS);
    }

    private static RiskLevel classify(BigDecimal score, BigDecimal maxExcess, BigDecimal hardThreshold) {
        if (maxExcess.compareTo(hardThreshold) >= 0) {
            return RiskLevel.HIGH;
        }
        if (score.compareTo(BigDecimal.ZERO) == 0) {
            return RiskLevel.NONE;
        }
        if (score.compareTo(SCORE_HIGH) < 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }

    private static LikelyRoute fallbackRoute(RiskLevel level) {
        return switch (level) {
            case NONE -> new LikelyRoute(false, false);
            case MEDIUM -> new LikelyRoute(true, false);
            case HIGH -> new LikelyRoute(true, true);
        };
    }

    public Map<Long, LineRiskResult> resultsByLineId(RiskEvaluation evaluation) {
        Map<Long, LineRiskResult> map = new HashMap<>();
        for (LineRiskResult result : evaluation.lines()) {
            map.put(result.lineId(), result);
        }
        return map;
    }
}

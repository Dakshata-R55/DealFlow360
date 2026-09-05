package com.dealflow360.policy.dto;

import com.dealflow360.policy.model.ApprovalPolicy;
import com.dealflow360.policy.model.RiskLevel;
import java.math.BigDecimal;

public record ApprovalPolicyResponse(
        long id,
        RiskLevel riskLevel,
        BigDecimal minScore,
        BigDecimal maxScore,
        boolean requiresManager,
        boolean requiresFinance,
        BigDecimal hardLineExcessThreshold) {

    public static ApprovalPolicyResponse from(ApprovalPolicy policy) {
        return new ApprovalPolicyResponse(
                policy.id(),
                policy.riskLevel(),
                policy.minScore(),
                policy.maxScore(),
                policy.requiresManager(),
                policy.requiresFinance(),
                policy.hardLineExcessThreshold());
    }
}

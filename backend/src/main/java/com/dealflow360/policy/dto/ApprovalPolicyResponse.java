package com.dealflow360.policy.dto;

import com.dealflow360.policy.model.ApprovalPolicy;
import java.math.BigDecimal;

public record ApprovalPolicyResponse(
        long id,
        BigDecimal managerLineExcessPercent,
        BigDecimal financeLineExcessPercent,
        BigDecimal managerQuoteExcessPercent,
        BigDecimal financeQuoteExcessPercent) {

    public static ApprovalPolicyResponse from(ApprovalPolicy policy) {
        return new ApprovalPolicyResponse(
                policy.id(),
                policy.managerLineExcessPercent(),
                policy.financeLineExcessPercent(),
                policy.managerQuoteExcessPercent(),
                policy.financeQuoteExcessPercent());
    }
}

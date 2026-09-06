package com.dealflow360.standing.dto;

import com.dealflow360.standing.model.StandingRule;
import java.math.BigDecimal;

public record StandingRuleResponse(
        long id, long companyId, BigDecimal silverMinSpend, BigDecimal goldMinSpend, int windowMonths) {

    public static StandingRuleResponse from(StandingRule rule) {
        return new StandingRuleResponse(
                rule.id(), rule.companyId(), rule.silverMinSpend(), rule.goldMinSpend(), rule.windowMonths());
    }
}

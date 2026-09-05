package com.dealflow360.upsell.dto;

import com.dealflow360.upsell.model.UpsellRule;
import java.math.BigDecimal;
import java.time.Instant;

public record UpsellRuleResponse(
        long id,
        long triggerProductId,
        long suggestedProductId,
        BigDecimal score,
        BigDecimal promotionBoost,
        BigDecimal minMarginPct,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static UpsellRuleResponse from(UpsellRule rule) {
        return new UpsellRuleResponse(
                rule.id(),
                rule.triggerProductId(),
                rule.suggestedProductId(),
                rule.score(),
                rule.promotionBoost(),
                rule.minMarginPct(),
                rule.active(),
                rule.createdAt(),
                rule.updatedAt());
    }
}

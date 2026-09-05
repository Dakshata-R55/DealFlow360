package com.dealflow360.upsell.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpsellRuleRequest(
        @NotNull Long triggerProductId,
        @NotNull Long suggestedProductId,
        @NotNull @DecimalMin("0") BigDecimal score,
        @NotNull @DecimalMin("0") BigDecimal promotionBoost,
        @NotNull @DecimalMin("0") BigDecimal minMarginPct,
        Boolean active) {}

package com.dealflow360.standing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StandingRuleRequest(
        @NotNull @DecimalMin("0") BigDecimal minSpend, @Min(1) @Max(24) Integer windowMonths) {}

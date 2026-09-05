package com.dealflow360.policy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DiscountPolicyRowRequest(
        Long customerTierId, Long categoryId, @NotNull @DecimalMin("0") BigDecimal maxDiscountPct) {}

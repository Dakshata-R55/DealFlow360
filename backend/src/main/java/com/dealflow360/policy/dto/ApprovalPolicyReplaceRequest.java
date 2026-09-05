package com.dealflow360.policy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApprovalPolicyReplaceRequest(
        @NotNull @DecimalMin("0") BigDecimal managerLineExcessPercent,
        @NotNull @DecimalMin("0") BigDecimal financeLineExcessPercent,
        @NotNull @DecimalMin("0") BigDecimal managerQuoteExcessPercent,
        @NotNull @DecimalMin("0") BigDecimal financeQuoteExcessPercent) {}

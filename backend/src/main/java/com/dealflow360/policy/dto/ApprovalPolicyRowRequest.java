package com.dealflow360.policy.dto;

import com.dealflow360.policy.model.RiskLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApprovalPolicyRowRequest(
        @NotNull RiskLevel riskLevel,
        @NotNull @DecimalMin("0") BigDecimal minScore,
        @NotNull @DecimalMin("0") BigDecimal maxScore,
        @NotNull Boolean requiresManager,
        @NotNull Boolean requiresFinance,
        @NotNull @DecimalMin("0") BigDecimal hardLineExcessThreshold) {}

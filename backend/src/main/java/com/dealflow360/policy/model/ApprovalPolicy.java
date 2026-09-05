package com.dealflow360.policy.model;

import java.math.BigDecimal;

public record ApprovalPolicy(
        long id,
        long companyId,
        RiskLevel riskLevel,
        BigDecimal minScore,
        BigDecimal maxScore,
        boolean requiresManager,
        boolean requiresFinance,
        BigDecimal hardLineExcessThreshold) {}

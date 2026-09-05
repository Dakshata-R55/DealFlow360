package com.dealflow360.policy.model;

import java.math.BigDecimal;

public record ApprovalPolicy(
        long id,
        long companyId,
        BigDecimal managerLineExcessPercent,
        BigDecimal financeLineExcessPercent,
        BigDecimal managerQuoteExcessPercent,
        BigDecimal financeQuoteExcessPercent) {}

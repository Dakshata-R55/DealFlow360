package com.dealflow360.standing.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StandingRule(
        long id,
        long companyId,
        BigDecimal silverMinSpend,
        BigDecimal goldMinSpend,
        int windowMonths,
        Instant createdAt,
        Instant updatedAt) {}

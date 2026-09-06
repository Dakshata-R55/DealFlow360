package com.dealflow360.standing.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StandingRule(
        long id,
        long companyId,
        long customerTierId,
        BigDecimal minSpend,
        int windowMonths,
        Instant createdAt,
        Instant updatedAt) {}

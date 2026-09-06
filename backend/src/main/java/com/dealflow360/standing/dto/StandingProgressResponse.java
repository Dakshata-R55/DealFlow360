package com.dealflow360.standing.dto;

import java.math.BigDecimal;

public record StandingProgressResponse(
        long sellerCompanyId,
        String sellerCompanyName,
        String standingName,
        BigDecimal spend,
        int windowMonths,
        BigDecimal silverMinSpend,
        BigDecimal goldMinSpend,
        String nextStanding,
        BigDecimal amountToNext) {}

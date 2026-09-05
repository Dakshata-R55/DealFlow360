package com.dealflow360.policy.model;

import java.math.BigDecimal;

public record DiscountPolicy(
        long id, long companyId, Long customerTierId, Long categoryId, BigDecimal maxDiscountPct) {}

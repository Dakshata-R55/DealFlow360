package com.dealflow360.policy.dto;

import com.dealflow360.policy.model.DiscountPolicy;
import java.math.BigDecimal;

public record DiscountPolicyResponse(
        long id, Long customerTierId, Long categoryId, BigDecimal maxDiscountPct) {

    public static DiscountPolicyResponse from(DiscountPolicy policy) {
        return new DiscountPolicyResponse(
                policy.id(), policy.customerTierId(), policy.categoryId(), policy.maxDiscountPct());
    }
}

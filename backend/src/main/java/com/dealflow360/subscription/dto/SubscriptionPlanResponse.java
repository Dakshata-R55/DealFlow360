package com.dealflow360.subscription.dto;

import com.dealflow360.subscription.model.PlanCycle;
import com.dealflow360.subscription.model.SubscriptionPlan;
import java.time.Instant;

public record SubscriptionPlanResponse(
        long id,
        String name,
        PlanCycle cycle,
        String prorationRule,
        String cancellationRule,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.id(),
                plan.name(),
                plan.cycle(),
                plan.prorationRule(),
                plan.cancellationRule(),
                plan.active(),
                plan.createdAt(),
                plan.updatedAt());
    }
}

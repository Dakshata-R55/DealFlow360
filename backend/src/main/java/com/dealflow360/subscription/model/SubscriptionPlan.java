package com.dealflow360.subscription.model;

import java.time.Instant;

public record SubscriptionPlan(
        long id,
        long companyId,
        String name,
        PlanCycle cycle,
        ProrationRule prorationRule,
        CancellationRule cancellationRule,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}

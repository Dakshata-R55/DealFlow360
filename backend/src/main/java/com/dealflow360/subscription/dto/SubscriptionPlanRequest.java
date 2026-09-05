package com.dealflow360.subscription.dto;

import com.dealflow360.subscription.model.CancellationRule;
import com.dealflow360.subscription.model.PlanCycle;
import com.dealflow360.subscription.model.ProrationRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPlanRequest(
        @NotBlank String name,
        @NotNull PlanCycle cycle,
        @NotNull ProrationRule prorationRule,
        @NotNull CancellationRule cancellationRule,
        Boolean active) {}

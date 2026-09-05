package com.dealflow360.subscription.dto;

import com.dealflow360.subscription.model.PlanCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPlanRequest(
        @NotBlank String name,
        @NotNull PlanCycle cycle,
        @NotBlank String prorationRule,
        @NotBlank String cancellationRule,
        Boolean active) {}

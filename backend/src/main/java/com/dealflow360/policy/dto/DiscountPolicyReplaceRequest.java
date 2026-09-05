package com.dealflow360.policy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record DiscountPolicyReplaceRequest(@NotNull List<@Valid DiscountPolicyRowRequest> policies) {}

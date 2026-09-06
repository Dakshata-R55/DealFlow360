package com.dealflow360.customer.dto;

import jakarta.validation.constraints.NotNull;

public record PatchCustomerTierRequest(@NotNull Long customerTierId) {}

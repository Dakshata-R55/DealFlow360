package com.dealflow360.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PriceListRequest(
        @NotBlank String name, @NotBlank String currency, @NotNull Long customerTierId, Boolean active) {}

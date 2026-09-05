package com.dealflow360.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceListItemRequest(@NotNull @DecimalMin("0") BigDecimal price) {}

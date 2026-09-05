package com.dealflow360.pricing.model;

import java.math.BigDecimal;

public record PriceListItem(long priceListId, long productId, BigDecimal price) {}

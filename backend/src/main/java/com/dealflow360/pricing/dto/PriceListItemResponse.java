package com.dealflow360.pricing.dto;

import com.dealflow360.pricing.model.PriceListItem;
import java.math.BigDecimal;

public record PriceListItemResponse(long priceListId, long productId, BigDecimal price) {

    public static PriceListItemResponse from(PriceListItem item) {
        return new PriceListItemResponse(item.priceListId(), item.productId(), item.price());
    }
}

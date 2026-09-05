package com.dealflow360.pricing.dto;

import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.model.PriceListItem;
import java.util.List;

public record PriceListResponse(
        long id,
        String name,
        String currency,
        long customerTierId,
        boolean active,
        List<PriceListItemResponse> items) {

    public static PriceListResponse from(PriceList priceList, List<PriceListItem> items) {
        return new PriceListResponse(
                priceList.id(),
                priceList.name(),
                priceList.currency(),
                priceList.customerTierId(),
                priceList.active(),
                items.stream().map(PriceListItemResponse::from).toList());
    }
}

package com.dealflow360.pricing.service;

import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.pricing.dto.CustomerTierRequest;
import com.dealflow360.pricing.dto.CustomerTierResponse;
import com.dealflow360.pricing.dto.PriceListItemRequest;
import com.dealflow360.pricing.dto.PriceListItemResponse;
import com.dealflow360.pricing.dto.PriceListRequest;
import com.dealflow360.pricing.dto.PriceListResponse;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.pricing.repository.PriceListRepository;
import com.dealflow360.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private final CustomerTierRepository tierRepository;
    private final PriceListRepository priceListRepository;
    private final ProductRepository productRepository;

    public PricingService(
            CustomerTierRepository tierRepository,
            PriceListRepository priceListRepository,
            ProductRepository productRepository) {
        this.tierRepository = tierRepository;
        this.priceListRepository = priceListRepository;
        this.productRepository = productRepository;
    }

    public List<CustomerTierResponse> listTiers(long companyId) {
        return tierRepository.findByCompany(companyId).stream()
                .map(CustomerTierResponse::from)
                .toList();
    }

    @Transactional
    public CustomerTierResponse createTier(long companyId, CustomerTierRequest request) {
        return CustomerTierResponse.from(tierRepository.insert(
                companyId,
                request.name().trim(),
                request.defaultDiscountLimit(),
                request.active() == null || request.active()));
    }

    public List<PriceListResponse> listPriceLists(long companyId) {
        return priceListRepository.findByCompany(companyId).stream()
                .map(list -> PriceListResponse.from(list, priceListRepository.findItems(list.id(), companyId)))
                .toList();
    }

    @Transactional
    public PriceListResponse createPriceList(long companyId, PriceListRequest request) {
        requireTier(companyId, request.customerTierId());
        PriceList created = priceListRepository.insert(
                companyId,
                request.name().trim(),
                request.currency().trim().toUpperCase(),
                request.customerTierId(),
                request.active() == null || request.active());
        return PriceListResponse.from(created, List.of());
    }

    @Transactional
    public PriceListItemResponse upsertItem(
            long companyId, long priceListId, long productId, PriceListItemRequest request) {
        PriceList priceList = priceListRepository
                .findById(priceListId, companyId)
                .orElseThrow(() -> new NotFoundException("Price list not found"));
        productRepository
                .findById(productId, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return PriceListItemResponse.from(
                priceListRepository.upsertItem(priceList.id(), productId, request.price()));
    }

    private void requireTier(long companyId, long tierId) {
        tierRepository.findById(tierId, companyId).orElseThrow(() -> new NotFoundException("Customer tier not found"));
    }
}

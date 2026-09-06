package com.dealflow360.pricing.service;

import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.pricing.dto.CustomerTierRequest;
import com.dealflow360.pricing.dto.CustomerTierResponse;
import com.dealflow360.pricing.dto.PriceListItemRequest;
import com.dealflow360.pricing.dto.PriceListItemResponse;
import com.dealflow360.pricing.dto.PriceListRequest;
import com.dealflow360.pricing.dto.PriceListResponse;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.pricing.repository.PriceListRepository;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.standing.service.StandingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private final CustomerTierRepository tierRepository;
    private final PriceListRepository priceListRepository;
    private final ProductRepository productRepository;
    private final StandingService standingService;

    public PricingService(
            CustomerTierRepository tierRepository,
            PriceListRepository priceListRepository,
            ProductRepository productRepository,
            StandingService standingService) {
        this.tierRepository = tierRepository;
        this.priceListRepository = priceListRepository;
        this.productRepository = productRepository;
        this.standingService = standingService;
    }

    public List<CustomerTierResponse> listTiers(long companyId) {
        return tierRepository.findByCompany(companyId).stream()
                .map(CustomerTierResponse::from)
                .toList();
    }

    @Transactional
    public CustomerTierResponse createTier(long companyId, CustomerTierRequest request) {
        CustomerTier tier = tierRepository.insert(
                companyId,
                request.name().trim(),
                request.defaultDiscountLimit(),
                request.active() == null || request.active());
        ensureDefaultPriceList(companyId, tier);
        standingService.ensureForTier(companyId, tier);
        return CustomerTierResponse.from(tier);
    }

    @Transactional
    public List<PriceListResponse> listPriceLists(long companyId) {
        ensureDefaultPriceLists(companyId);
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
    public PriceListResponse updatePriceList(long companyId, long id, PriceListRequest request) {
        priceListRepository
                .findById(id, companyId)
                .orElseThrow(() -> new NotFoundException("Price list not found"));
        requireTier(companyId, request.customerTierId());
        PriceList updated = priceListRepository
                .update(
                        id,
                        companyId,
                        request.name().trim(),
                        request.currency().trim().toUpperCase(),
                        request.customerTierId(),
                        request.active() == null || request.active())
                .orElseThrow(() -> new NotFoundException("Price list not found"));
        return PriceListResponse.from(updated, priceListRepository.findItems(updated.id(), companyId));
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

    private void ensureDefaultPriceLists(long companyId) {
        for (CustomerTier tier : tierRepository.findByCompany(companyId)) {
            ensureDefaultPriceList(companyId, tier);
        }
    }

    private void ensureDefaultPriceList(long companyId, CustomerTier tier) {
        if (!priceListRepository.findByTier(companyId, tier.id()).isEmpty()) {
            return;
        }
        priceListRepository.insert(companyId, tier.name() + " INR", "INR", tier.id(), true);
    }
}

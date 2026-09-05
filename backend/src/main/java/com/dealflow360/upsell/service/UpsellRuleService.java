package com.dealflow360.upsell.service;

import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.upsell.dto.UpsellRuleRequest;
import com.dealflow360.upsell.dto.UpsellRuleResponse;
import com.dealflow360.upsell.repository.UpsellRuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpsellRuleService {

    private final UpsellRuleRepository upsellRuleRepository;
    private final ProductRepository productRepository;

    public UpsellRuleService(UpsellRuleRepository upsellRuleRepository, ProductRepository productRepository) {
        this.upsellRuleRepository = upsellRuleRepository;
        this.productRepository = productRepository;
    }

    public List<UpsellRuleResponse> list(long companyId) {
        return upsellRuleRepository.findByCompany(companyId).stream()
                .map(UpsellRuleResponse::from)
                .toList();
    }

    @Transactional
    public UpsellRuleResponse create(long companyId, UpsellRuleRequest request) {
        if (request.triggerProductId().equals(request.suggestedProductId())) {
            throw new BadRequestException("Trigger and suggested products must differ");
        }
        requireProduct(companyId, request.triggerProductId());
        requireProduct(companyId, request.suggestedProductId());
        return UpsellRuleResponse.from(upsellRuleRepository.insert(
                companyId,
                request.triggerProductId(),
                request.suggestedProductId(),
                request.score(),
                request.promotionBoost(),
                request.minMarginPct(),
                request.active() == null || request.active()));
    }

    private void requireProduct(long companyId, long productId) {
        productRepository
                .findById(productId, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }
}

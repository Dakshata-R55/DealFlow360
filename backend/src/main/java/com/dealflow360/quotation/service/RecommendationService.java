package com.dealflow360.quotation.service;

import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.quotation.dto.RecommendationResponse;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationLine;
import com.dealflow360.quotation.repository.QuotationRepository;
import com.dealflow360.upsell.model.UpsellRule;
import com.dealflow360.upsell.repository.UpsellRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final int TOP_N = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final UpsellRuleRepository upsellRuleRepository;
    private final ProductRepository productRepository;
    private final QuotationRepository quotationRepository;
    private final QuotePricingService quotePricingService;

    public RecommendationService(
            UpsellRuleRepository upsellRuleRepository,
            ProductRepository productRepository,
            QuotationRepository quotationRepository,
            QuotePricingService quotePricingService) {
        this.upsellRuleRepository = upsellRuleRepository;
        this.productRepository = productRepository;
        this.quotationRepository = quotationRepository;
        this.quotePricingService = quotePricingService;
    }

    public List<RecommendationResponse> recommend(long companyId, Quotation quotation, List<QuotationLine> lines) {
        Set<Long> onQuote = new HashSet<>();
        Set<Long> triggers = new HashSet<>();
        for (QuotationLine line : lines) {
            onQuote.add(line.productId());
            triggers.add(line.productId());
        }
        Set<Long> dismissed = quotationRepository.findDismissedProductIds(quotation.id());
        List<UpsellRule> rules = upsellRuleRepository.findActiveByCompanyAndTriggerIds(companyId, triggers);
        List<Scored> scored = new ArrayList<>();
        Set<Long> seenSuggested = new HashSet<>();
        for (UpsellRule rule : rules) {
            if (onQuote.contains(rule.suggestedProductId()) || dismissed.contains(rule.suggestedProductId())) {
                continue;
            }
            if (seenSuggested.contains(rule.suggestedProductId())) {
                continue;
            }
            Product suggested = productRepository
                    .findById(rule.suggestedProductId(), companyId)
                    .orElse(null);
            if (suggested == null || !suggested.active()) {
                continue;
            }
            BigDecimal unitPrice =
                    quotePricingService.resolveUnitPrice(companyId, quotation.priceListId(), suggested, null);
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal expectedMarginPct = QuotePricingService.percent(unitPrice.subtract(suggested.costPrice()), unitPrice);
            if (expectedMarginPct.compareTo(rule.minMarginPct()) < 0) {
                continue;
            }
            BigDecimal normalizedMargin = expectedMarginPct.divide(new BigDecimal("100"), 4, ROUNDING);
            BigDecimal score = rule.score().add(rule.promotionBoost()).add(normalizedMargin);
            BigDecimal marginDelta = QuotePricingService.money(unitPrice.subtract(suggested.costPrice()));
            scored.add(new Scored(
                    new RecommendationResponse(
                            suggested.id(),
                            suggested.name(),
                            rule.promotionBoost().compareTo(BigDecimal.ZERO) > 0,
                            marginDelta,
                            score.setScale(4, ROUNDING)),
                    score));
            seenSuggested.add(suggested.id());
        }
        scored.sort(Comparator.comparing((Scored item) -> item.score).reversed());
        return scored.stream().limit(TOP_N).map(item -> item.response).toList();
    }

    private record Scored(RecommendationResponse response, BigDecimal score) {}
}

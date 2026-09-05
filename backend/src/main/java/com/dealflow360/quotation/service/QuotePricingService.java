package com.dealflow360.quotation.service;

import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductVariant;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.PriceListRepository;
import com.dealflow360.quotation.model.QuotationLine;
import com.dealflow360.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QuotePricingService {

    private static final int MONEY_SCALE = 2;
    private static final int PCT_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record LineCommercial(
            BigDecimal quantity,
            BigDecimal discountPercent,
            BigDecimal gross,
            BigDecimal discountAmount,
            BigDecimal lineTotal,
            BigDecimal lineCost,
            BigDecimal marginAmount,
            BigDecimal marginPercent) {}

    public record QuoteTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal totalCost,
            BigDecimal marginAmount,
            BigDecimal marginPercent) {}

    private final PriceListRepository priceListRepository;

    public QuotePricingService(PriceListRepository priceListRepository) {
        this.priceListRepository = priceListRepository;
    }

    public PriceList requirePriceList(long companyId, long customerTierId) {
        return priceListRepository
                .findActiveByCompanyAndTier(companyId, customerTierId)
                .orElseThrow(() -> new BadRequestException("No active price list for customer tier"));
    }

    public BigDecimal resolveUnitPrice(
            long companyId, long priceListId, Product product, ProductVariant variant) {
        BigDecimal listOrBase = priceListRepository
                .findItem(priceListId, product.id(), companyId)
                .map(item -> item.price())
                .orElse(product.basePrice());
        BigDecimal extra = variant == null ? BigDecimal.ZERO : variant.extraPrice();
        return money(listOrBase.add(extra));
    }

    public LineCommercial commercial(
            BigDecimal quantity, BigDecimal resolvedUnitPrice, BigDecimal costPrice, BigDecimal discountPercent) {
        BigDecimal gross = money(quantity.multiply(resolvedUnitPrice));
        BigDecimal discountAmount = money(gross.multiply(discountPercent).divide(HUNDRED, MONEY_SCALE + 4, ROUNDING));
        BigDecimal lineTotal = money(gross.subtract(discountAmount));
        BigDecimal lineCost = money(quantity.multiply(costPrice));
        BigDecimal marginAmount = money(lineTotal.subtract(lineCost));
        BigDecimal marginPercent = percent(marginAmount, lineTotal);
        return new LineCommercial(
                quantity, discountPercent, gross, discountAmount, lineTotal, lineCost, marginAmount, marginPercent);
    }

    public QuoteTotals totals(List<QuotationLine> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (QuotationLine line : lines) {
            subtotal = subtotal.add(line.lineTotal().add(line.discountAmount()));
            discountAmount = discountAmount.add(line.discountAmount());
            totalAmount = totalAmount.add(line.lineTotal());
            totalCost = totalCost.add(line.quantity().multiply(line.costPrice()));
        }
        subtotal = money(subtotal);
        discountAmount = money(discountAmount);
        totalAmount = money(totalAmount);
        totalCost = money(totalCost);
        BigDecimal marginAmount = money(totalAmount.subtract(totalCost));
        return new QuoteTotals(
                subtotal, discountAmount, totalAmount, totalCost, marginAmount, percent(marginAmount, totalAmount));
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, ROUNDING);
        }
        return numerator.multiply(HUNDRED).divide(denominator, PCT_SCALE, ROUNDING);
    }
}

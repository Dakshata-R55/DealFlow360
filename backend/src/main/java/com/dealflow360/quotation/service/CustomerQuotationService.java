package com.dealflow360.quotation.service;

import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.fulfillment.service.FulfillmentService;
import com.dealflow360.fulfillment.service.FulfillmentService.FulfillmentSummary;
import com.dealflow360.quotation.dto.CustomerCounterRequest;
import com.dealflow360.quotation.dto.CustomerQuotationLineResponse;
import com.dealflow360.quotation.dto.CustomerQuotationResponse;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationLine;
import com.dealflow360.quotation.model.QuotationStatus;
import com.dealflow360.quotation.repository.QuotationLineRepository;
import com.dealflow360.quotation.repository.QuotationRepository;
import com.dealflow360.quoterequest.model.QuoteRequest;
import com.dealflow360.quoterequest.model.QuoteRequestLine;
import com.dealflow360.quoterequest.repository.QuoteRequestLineRepository;
import com.dealflow360.quoterequest.repository.QuoteRequestRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.standing.service.StandingService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerQuotationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final QuoteRequestRepository quoteRequestRepository;
    private final QuoteRequestLineRepository quoteRequestLineRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationLineRepository quotationLineRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final FulfillmentService fulfillmentService;
    private final StandingService standingService;

    public CustomerQuotationService(
            QuoteRequestRepository quoteRequestRepository,
            QuoteRequestLineRepository quoteRequestLineRepository,
            QuotationRepository quotationRepository,
            QuotationLineRepository quotationLineRepository,
            ProductRepository productRepository,
            CompanyRepository companyRepository,
            FulfillmentService fulfillmentService,
            StandingService standingService) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.quoteRequestLineRepository = quoteRequestLineRepository;
        this.quotationRepository = quotationRepository;
        this.quotationLineRepository = quotationLineRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.fulfillmentService = fulfillmentService;
        this.standingService = standingService;
    }

    public List<CustomerQuotationResponse> list(long customerUserId) {
        List<CustomerQuotationResponse> rows = new ArrayList<>();
        for (QuoteRequest request : quoteRequestRepository.findByCustomer(customerUserId)) {
            if (request.quotationId() == null) {
                continue;
            }
            rows.add(toResponse(owned(customerUserId, request.quotationId())));
        }
        return rows;
    }

    public CustomerQuotationResponse get(long customerUserId, long quotationId) {
        return toResponse(owned(customerUserId, quotationId));
    }

    @Transactional
    public CustomerQuotationResponse counter(long customerUserId, long quotationId, CustomerCounterRequest body) {
        Owned owned = owned(customerUserId, quotationId);
        if (owned.quotation().status() != QuotationStatus.NEGOTIATION) {
            throw new ConflictException("Only a negotiation offer can be countered");
        }
        validateExpected(body.expectedDiscountPercent());
        quoteRequestRepository.updateExpectedDiscount(owned.request().id(), body.expectedDiscountPercent());
        if (body.lines() != null) {
            for (CustomerCounterRequest.CustomerCounterLine line : body.lines()) {
                validateExpected(line.expectedDiscountPercent());
                QuoteRequestLine existing = quoteRequestLineRepository.findByRequest(owned.request().id()).stream()
                        .filter(row -> row.productId() == line.productId())
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Product is not on this request"));
                quoteRequestLineRepository.update(
                        existing.id(),
                        owned.request().id(),
                        existing.quantity(),
                        existing.notes(),
                        line.expectedDiscountPercent());
            }
        }
        quotationRepository.updateApproval(quotationId, owned.request().sellerCompanyId(), null, null);
        return toResponse(owned(customerUserId, quotationId));
    }

    @Transactional
    public CustomerQuotationResponse confirmCredit(long customerUserId, long quotationId) {
        Owned owned = owned(customerUserId, quotationId);
        if (owned.quotation().status() != QuotationStatus.APPROVED) {
            throw new ConflictException("Only an approved quotation can be confirmed on credit");
        }
        quotationRepository.updateStatus(
                quotationId,
                owned.request().sellerCompanyId(),
                QuotationStatus.CONFIRMED,
                owned.quotation().submittedAt(),
                owned.quotation().riskScore(),
                owned.quotation().riskLevel());
        fulfillmentService.takeOnConfirm(owned.request().sellerCompanyId(), quotationId);
        standingService.evaluateAfterConfirm(owned.request().sellerCompanyId(), owned.quotation().customerId());
        return toResponse(owned(customerUserId, quotationId));
    }

    private Owned owned(long customerUserId, long quotationId) {
        QuoteRequest request = quoteRequestRepository
                .findByQuotationId(quotationId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        if (request.customerUserId() != customerUserId) {
            throw new NotFoundException("Quotation not found");
        }
        Quotation quotation = quotationRepository
                .findById(quotationId, request.sellerCompanyId())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        return new Owned(request, quotation);
    }

    private CustomerQuotationResponse toResponse(Owned owned) {
        Company seller = companyRepository.findById(owned.request().sellerCompanyId()).orElse(null);
        List<CustomerQuotationLineResponse> lines = new ArrayList<>();
        for (QuotationLine line : quotationLineRepository.findByQuotation(owned.quotation().id())) {
            Product product = productRepository
                    .findById(line.productId(), owned.request().sellerCompanyId())
                    .orElse(null);
            lines.add(new CustomerQuotationLineResponse(
                    line.id(),
                    line.productId(),
                    product == null ? "Product" : product.name(),
                    line.quantity(),
                    line.resolvedUnitPrice(),
                    line.discountPercent(),
                    line.lineTotal(),
                    line.billingType()));
        }
        FulfillmentSummary fulfillment = owned.quotation().status() == QuotationStatus.CONFIRMED
                ? fulfillmentService.summary(owned.request().sellerCompanyId(), owned.quotation().id())
                : new FulfillmentSummary(0, 0, List.of());
        return new CustomerQuotationResponse(
                owned.quotation().id(),
                owned.quotation().quoteNumber(),
                owned.request().sellerCompanyId(),
                seller == null ? "" : seller.name(),
                owned.quotation().status(),
                customerStatusLabel(owned.quotation().status()),
                owned.quotation().subtotal(),
                owned.quotation().discountAmount(),
                owned.quotation().totalAmount(),
                owned.request().id(),
                owned.request().requestNumber(),
                owned.request().expectedDiscountPercent(),
                fulfillment.shipQty(),
                fulfillment.backorderQty(),
                fulfillment.shipFrom(),
                lines);
    }

    private static String customerStatusLabel(QuotationStatus status) {
        return switch (status) {
            case DRAFT, PENDING_APPROVAL -> "Seller working";
            case NEGOTIATION -> "Under Negotiation";
            case APPROVED -> "Ready to confirm";
            case CONFIRMED -> "Confirmed";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private static void validateExpected(BigDecimal expected) {
        if (expected == null) {
            return;
        }
        if (expected.compareTo(BigDecimal.ZERO) < 0 || expected.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Expected discount must be between 0 and 100");
        }
    }

    private record Owned(QuoteRequest request, Quotation quotation) {}
}

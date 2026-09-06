package com.dealflow360.quotation.service;

import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductVariant;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.catalog.repository.ProductVariantRepository;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.fulfillment.service.FulfillmentService;
import com.dealflow360.policy.model.RiskLevel;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.pricing.repository.PriceListRepository;
import com.dealflow360.quotation.dto.AddQuotationLineRequest;
import com.dealflow360.quotation.dto.CreateQuotationRequest;
import com.dealflow360.quotation.dto.PatchAssigneeRequest;
import com.dealflow360.quotation.dto.PatchQuotationLineRequest;
import com.dealflow360.quotation.dto.QuotationLineResponse;
import com.dealflow360.quotation.dto.QuotationResponse;
import com.dealflow360.quotation.dto.QuotationResponse.LikelyRoute;
import com.dealflow360.quotation.dto.RecommendationResponse;
import com.dealflow360.quotation.dto.SalesUserResponse;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationLine;
import com.dealflow360.quotation.model.QuotationStatus;
import com.dealflow360.quotation.repository.QuotationLineRepository;
import com.dealflow360.quotation.repository.QuotationRepository;
import com.dealflow360.quoterequest.model.QuoteRequest;
import com.dealflow360.quoterequest.model.QuoteRequestLine;
import com.dealflow360.quoterequest.model.QuoteRequestStatus;
import com.dealflow360.quoterequest.repository.QuoteRequestLineRepository;
import com.dealflow360.quoterequest.repository.QuoteRequestRepository;
import com.dealflow360.quoterequest.service.QuoteRequestService;
import com.dealflow360.policy.model.DiscountPolicy;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final QuotationRepository quotationRepository;
    private final QuotationLineRepository lineRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository tierRepository;
    private final PriceListRepository priceListRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final QuotePricingService quotePricingService;
    private final RiskEngine riskEngine;
    private final RecommendationService recommendationService;
    private final QuoteRequestRepository quoteRequestRepository;
    private final QuoteRequestLineRepository quoteRequestLineRepository;
    private final UserRepository userRepository;
    private final FulfillmentService fulfillmentService;

    public QuotationService(
            QuotationRepository quotationRepository,
            QuotationLineRepository lineRepository,
            CustomerRepository customerRepository,
            CustomerTierRepository tierRepository,
            PriceListRepository priceListRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            QuotePricingService quotePricingService,
            RiskEngine riskEngine,
            RecommendationService recommendationService,
            QuoteRequestRepository quoteRequestRepository,
            QuoteRequestLineRepository quoteRequestLineRepository,
            UserRepository userRepository,
            FulfillmentService fulfillmentService) {
        this.quotationRepository = quotationRepository;
        this.lineRepository = lineRepository;
        this.customerRepository = customerRepository;
        this.tierRepository = tierRepository;
        this.priceListRepository = priceListRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.quotePricingService = quotePricingService;
        this.riskEngine = riskEngine;
        this.recommendationService = recommendationService;
        this.quoteRequestRepository = quoteRequestRepository;
        this.quoteRequestLineRepository = quoteRequestLineRepository;
        this.userRepository = userRepository;
        this.fulfillmentService = fulfillmentService;
    }

    public List<SalesUserResponse> listSalesUsers(long companyId) {
        List<SalesUserResponse> rows = new ArrayList<>();
        for (User user : userRepository.findActiveSalesByCompany(companyId)) {
            rows.add(SalesUserResponse.from(user));
        }
        return rows;
    }

    public List<QuotationResponse> list(long companyId) {
        List<QuotationResponse> rows = new ArrayList<>();
        for (Quotation quotation : quotationRepository.findByCompany(companyId)) {
            rows.add(toResponse(companyId, quotation, List.of()));
        }
        return rows;
    }

    public QuotationResponse get(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        return toResponse(companyId, quotation, lineRepository.findByQuotation(quotation.id()));
    }

    @Transactional
    public QuotationResponse create(long companyId, long salesRepId, CreateQuotationRequest request) {
        Customer customer = customerRepository
                .findById(request.customerId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!customer.active()) {
            throw new BadRequestException("Customer is inactive");
        }
        CustomerTier tier = requireTier(companyId, customer.customerTierId());
        PriceList priceList = quotePricingService.requirePriceList(companyId, tier.id());
        Quotation created = quotationRepository.insert(
                companyId, customer.id(), salesRepId, priceList.id(), QuotationStatus.DRAFT);
        return toResponse(companyId, created, List.of());
    }

    @Transactional
    public QuotationResponse addLine(long companyId, long quotationId, AddQuotationLineRequest request) {
        Quotation quotation = requireDraft(companyId, quotationId);
        Product product = productRepository
                .findById(request.productId(), companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.active()) {
            throw new BadRequestException("Product is inactive");
        }
        ProductVariant variant = requireVariant(companyId, product.id(), request.variantId());
        BigDecimal unitPrice =
                quotePricingService.resolveUnitPrice(companyId, quotation.priceListId(), product, variant);
        BigDecimal discountPercent = request.discountPercent() == null ? BigDecimal.ZERO : request.discountPercent();
        validateDiscount(discountPercent);
        QuotePricingService.LineCommercial commercial = quotePricingService.commercial(
                request.quantity(), unitPrice, product.costPrice(), discountPercent);
        lineRepository.insert(
                quotation.id(),
                product.id(),
                variant == null ? null : variant.id(),
                request.quantity(),
                QuotePricingService.money(product.basePrice()),
                unitPrice,
                QuotePricingService.money(product.costPrice()),
                discountPercent,
                commercial.discountAmount(),
                BigDecimal.ZERO,
                commercial.lineTotal(),
                commercial.marginAmount(),
                commercial.marginPercent(),
                product.billingType());
        return recalculate(companyId, quotation.id());
    }

    @Transactional
    public QuotationResponse updateLine(
            long companyId, long quotationId, long lineId, PatchQuotationLineRequest request) {
        requireDraft(companyId, quotationId);
        QuotationLine existing = lineRepository
                .findById(lineId, quotationId)
                .orElseThrow(() -> new NotFoundException("Quotation line not found"));
        if (request.quantity() == null && request.discountPercent() == null) {
            throw new BadRequestException("quantity or discountPercent is required");
        }
        BigDecimal quantity = request.quantity() == null ? existing.quantity() : request.quantity();
        BigDecimal discountPercent =
                request.discountPercent() == null ? existing.discountPercent() : request.discountPercent();
        validateDiscount(discountPercent);
        QuotePricingService.LineCommercial commercial = quotePricingService.commercial(
                quantity, existing.resolvedUnitPrice(), existing.costPrice(), discountPercent);
        lineRepository.updateComputed(new QuotationLine(
                existing.id(),
                existing.quotationId(),
                existing.productId(),
                existing.variantId(),
                quantity,
                existing.baseUnitPrice(),
                existing.resolvedUnitPrice(),
                existing.costPrice(),
                discountPercent,
                commercial.discountAmount(),
                existing.allowedDiscountPercent(),
                commercial.lineTotal(),
                commercial.marginAmount(),
                commercial.marginPercent(),
                existing.billingType(),
                existing.createdAt(),
                existing.updatedAt()));
        return recalculate(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse deleteLine(long companyId, long quotationId, long lineId) {
        requireDraft(companyId, quotationId);
        if (!lineRepository.delete(lineId, quotationId)) {
            throw new NotFoundException("Quotation line not found");
        }
        return recalculate(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse evaluate(long companyId, long quotationId) {
        requireQuote(companyId, quotationId);
        return recalculate(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse saveDraft(long companyId, long quotationId) {
        requireDraft(companyId, quotationId);
        return recalculate(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse submit(long companyId, long quotationId) {
        requireDraft(companyId, quotationId);
        List<QuotationLine> lines = lineRepository.findByQuotation(quotationId);
        if (lines.isEmpty()) {
            throw new BadRequestException("Quotation must have at least one line");
        }
        QuotationResponse evaluated = recalculate(companyId, quotationId);
        QuotationStatus next = evaluated.riskLevel() == RiskLevel.NONE
                ? QuotationStatus.APPROVED
                : QuotationStatus.PENDING_APPROVAL;
        Quotation submitted = quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        next,
                        Instant.now(),
                        evaluated.riskScore(),
                        evaluated.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        clearApproval(companyId, quotationId);
        if (next == QuotationStatus.APPROVED) {
            fulfillmentService.reserveOnApprove(companyId, quotationId);
        }
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse assign(long companyId, long quotationId, PatchAssigneeRequest request) {
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() == QuotationStatus.CONFIRMED) {
            throw new ConflictException("Confirmed quotations cannot be reassigned");
        }
        User assignee = requireSalesUser(companyId, request.salesRepId());
        Quotation updated = quotationRepository
                .updateSalesRep(quotationId, companyId, assignee.id())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        return toResponse(companyId, updated, lineRepository.findByQuotation(quotationId));
    }

    @Transactional
    public QuotationResponse reopen(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() != QuotationStatus.PENDING_APPROVAL
                && quotation.status() != QuotationStatus.NEGOTIATION
                && quotation.status() != QuotationStatus.APPROVED) {
            throw new ConflictException("Only pending approval, negotiation, or approved can return to draft");
        }
        if (quotation.status() == QuotationStatus.APPROVED) {
            fulfillmentService.releaseHold(companyId, quotationId);
        }
        Quotation updated = quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.DRAFT,
                        null,
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        clearApproval(companyId, quotationId);
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse returnToQueue(long companyId, long quotationId) {
        Quotation quotation = requireDraft(companyId, quotationId);
        QuoteRequest request = quoteRequestRepository
                .findByQuotationId(quotation.id())
                .orElseThrow(() -> new ConflictException("Only a converted request can return to To do"));
        if (request.sellerCompanyId() != companyId) {
            throw new NotFoundException("Quotation not found");
        }
        quoteRequestRepository.updateStatus(
                request.id(), QuoteRequestStatus.UNDER_REVIEW, request.submittedAt(), null);
        quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.CANCELLED,
                        quotation.submittedAt(),
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse cancel(long companyId, long quotationId) {
        Quotation quotation = requireDraft(companyId, quotationId);
        if (quoteRequestRepository.findByQuotationId(quotation.id()).isPresent()) {
            return returnToQueue(companyId, quotationId);
        }
        quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.CANCELLED,
                        quotation.submittedAt(),
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse returnToPending(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() != QuotationStatus.APPROVED && quotation.status() != QuotationStatus.NEGOTIATION) {
            throw new ConflictException("Only approved or negotiation can return to pending approval");
        }
        if (quotation.status() == QuotationStatus.APPROVED) {
            fulfillmentService.releaseHold(companyId, quotationId);
        }
        quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.PENDING_APPROVAL,
                        quotation.submittedAt(),
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        clearApproval(companyId, quotationId);
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse approve(long companyId, long quotationId, UserRole role) {
        if (role != UserRole.SALES_MANAGER && role != UserRole.FINANCE_OPS) {
            throw new ConflictException("Only Sales Manager or Finance can approve");
        }
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() != QuotationStatus.PENDING_APPROVAL
                && quotation.status() != QuotationStatus.NEGOTIATION) {
            throw new ConflictException("Only a pending or negotiation quotation can be approved");
        }
        if (quotation.riskLevel() == RiskLevel.HIGH && role != UserRole.FINANCE_OPS) {
            throw new ConflictException("Finance must approve high-risk quotes");
        }
        Instant now = Instant.now();
        Instant managerAt = quotation.managerApprovedAt();
        Instant financeAt = quotation.financeApprovedAt();
        if (role == UserRole.SALES_MANAGER) {
            managerAt = now;
        } else {
            financeAt = now;
        }
        quotationRepository.updateApproval(quotationId, companyId, managerAt, financeAt);
        Quotation approved = quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.APPROVED,
                        quotation.submittedAt(),
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        fulfillmentService.reserveOnApprove(companyId, quotationId);
        return toResponse(companyId, approved, lineRepository.findByQuotation(quotationId));
    }

    @Transactional
    public QuotationResponse negotiate(long companyId, long quotationId, UserRole role) {
        if (role != UserRole.SALES_MANAGER && role != UserRole.FINANCE_OPS) {
            throw new ConflictException("Only Sales Manager or Finance can send a quote to negotiation");
        }
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() != QuotationStatus.PENDING_APPROVAL && quotation.status() != QuotationStatus.APPROVED) {
            throw new ConflictException("Negotiation is only from pending approval or approved");
        }
        if (quotation.status() == QuotationStatus.APPROVED) {
            fulfillmentService.releaseHold(companyId, quotationId);
        }
        Quotation updated = quotationRepository
                .updateStatus(
                        quotationId,
                        companyId,
                        QuotationStatus.NEGOTIATION,
                        quotation.submittedAt(),
                        quotation.riskScore(),
                        quotation.riskLevel())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        clearApproval(companyId, quotationId);
        return get(companyId, quotationId);
    }

    @Transactional
    public QuotationResponse confirmCredit(long companyId, long quotationId) {
        throw new ConflictException("Customer confirms credit from the portal");
    }

    private void clearApproval(long companyId, long quotationId) {
        quotationRepository.updateApproval(quotationId, companyId, null, null);
    }

    @Transactional
    public List<RecommendationResponse> recommendations(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        return recommendationService.recommend(companyId, quotation, lineRepository.findByQuotation(quotation.id()));
    }

    @Transactional
    public List<RecommendationResponse> dismissRecommendation(long companyId, long quotationId, long productId) {
        Quotation quotation = requireDraft(companyId, quotationId);
        productRepository.findById(productId, companyId).orElseThrow(() -> new NotFoundException("Product not found"));
        quotationRepository.insertDismissal(quotation.id(), productId);
        return recommendationService.recommend(companyId, quotation, lineRepository.findByQuotation(quotation.id()));
    }

    private QuotationResponse recalculate(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        Customer customer = customerRepository
                .findById(quotation.customerId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerTier tier = requireTier(companyId, customer.customerTierId());
        List<QuotationLine> lines = lineRepository.findByQuotation(quotation.id());
        List<RiskEngine.LineRiskInput> inputs = new ArrayList<>();
        for (QuotationLine line : lines) {
            Product product = productRepository
                    .findById(line.productId(), companyId)
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            QuotePricingService.LineCommercial commercial = quotePricingService.commercial(
                    line.quantity(), line.resolvedUnitPrice(), line.costPrice(), line.discountPercent());
            BigDecimal baseValue = commercial.gross();
            inputs.add(new RiskEngine.LineRiskInput(
                    line.id(), product.categoryId(), baseValue, line.discountPercent()));
            lineRepository.updateComputed(new QuotationLine(
                    line.id(),
                    line.quotationId(),
                    line.productId(),
                    line.variantId(),
                    line.quantity(),
                    line.baseUnitPrice(),
                    line.resolvedUnitPrice(),
                    line.costPrice(),
                    line.discountPercent(),
                    commercial.discountAmount(),
                    line.allowedDiscountPercent(),
                    commercial.lineTotal(),
                    commercial.marginAmount(),
                    commercial.marginPercent(),
                    line.billingType(),
                    line.createdAt(),
                    line.updatedAt()));
        }
        lines = lineRepository.findByQuotation(quotation.id());
        RiskEngine.RiskEvaluation evaluation = riskEngine.evaluate(companyId, tier, inputs);
        Map<Long, RiskEngine.LineRiskResult> byLine = riskEngine.resultsByLineId(evaluation);
        List<QuotationLine> persisted = new ArrayList<>();
        for (QuotationLine line : lines) {
            RiskEngine.LineRiskResult risk = byLine.get(line.id());
            BigDecimal allowed = risk == null ? line.allowedDiscountPercent() : risk.allowedDiscount();
            QuotationLine withAllowed = new QuotationLine(
                    line.id(),
                    line.quotationId(),
                    line.productId(),
                    line.variantId(),
                    line.quantity(),
                    line.baseUnitPrice(),
                    line.resolvedUnitPrice(),
                    line.costPrice(),
                    line.discountPercent(),
                    line.discountAmount(),
                    allowed,
                    line.lineTotal(),
                    line.marginAmount(),
                    line.marginPercent(),
                    line.billingType(),
                    line.createdAt(),
                    line.updatedAt());
            persisted.add(lineRepository.updateComputed(withAllowed).orElse(withAllowed));
        }
        QuotePricingService.QuoteTotals totals = quotePricingService.totals(persisted);
        Quotation updated = quotationRepository
                .updateComputed(
                        quotation.id(),
                        companyId,
                        totals.subtotal(),
                        totals.discountAmount(),
                        totals.totalAmount(),
                        totals.totalCost(),
                        totals.marginAmount(),
                        totals.marginPercent(),
                        evaluation.quoteExcessPct(),
                        evaluation.level())
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        return toResponse(companyId, updated, persisted, evaluation.likelyRoute());
    }

    private QuotationResponse toResponse(long companyId, Quotation quotation, List<QuotationLine> lines) {
        return toResponse(companyId, quotation, lines, riskEngine.routeFor(quotation.riskLevel()));
    }

    private QuotationResponse toResponse(
            long companyId, Quotation quotation, List<QuotationLine> lines, LikelyRoute likelyRoute) {
        Customer customer = customerRepository
                .findById(quotation.customerId(), companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerTier tier = requireTier(companyId, customer.customerTierId());
        PriceList priceList = priceListRepository
                .findById(quotation.priceListId(), companyId)
                .orElseThrow(() -> new NotFoundException("Price list not found"));
        String salesRepName = userRepository
                .findById(quotation.salesRepId())
                .map(User::name)
                .orElse("Unknown");
        List<QuotationLineResponse> lineResponses = new ArrayList<>();
        QuoteRequest source = quoteRequestRepository.findByQuotationId(quotation.id()).orElse(null);
        Map<Long, Deque<QuoteRequestLine>> expectedByProduct = new HashMap<>();
        if (source != null) {
            for (QuoteRequestLine requestLine : quoteRequestLineRepository.findByRequest(source.id())) {
                expectedByProduct
                        .computeIfAbsent(requestLine.productId(), ignored -> new ArrayDeque<>())
                        .add(requestLine);
            }
        }
        List<DiscountPolicy> policies = riskEngine.policies(companyId);
        for (QuotationLine line : lines) {
            Product product = productRepository
                    .findById(line.productId(), companyId)
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            String variantLabel = null;
            if (line.variantId() != null) {
                variantLabel = variantRepository
                        .findById(line.variantId(), companyId)
                        .map(variant -> variant.attributeName() + " " + variant.attributeValue())
                        .orElse(null);
            }
            QuoteRequestLine requestLine =
                    expectedByProduct.getOrDefault(line.productId(), new ArrayDeque<>()).pollFirst();
            BigDecimal available = riskEngine.allowedDiscount(companyId, tier, product.categoryId(), policies);
            BigDecimal customerExpected = null;
            boolean expectedIsDefault = true;
            if (source != null) {
                BigDecimal stored = requestLine == null ? null : requestLine.expectedDiscountPercent();
                customerExpected = QuoteRequestService.appliedExpected(
                        stored, source.expectedDiscountPercent(), available);
                expectedIsDefault = !QuoteRequestService.isIndependentExpected(stored, available);
            }
            lineResponses.add(QuotationLineResponse.from(
                    line, product.name(), variantLabel, customerExpected, expectedIsDefault));
        }
        return QuotationResponse.from(
                quotation,
                customer.name(),
                tier.id(),
                tier.name(),
                salesRepName,
                priceList.name(),
                likelyRoute,
                lineResponses,
                source == null ? null : source.requestNumber(),
                source == null ? null : source.expectedDiscountPercent());
    }

    private Quotation requireQuote(long companyId, long quotationId) {
        return quotationRepository
                .findById(quotationId, companyId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
    }

    private Quotation requireDraft(long companyId, long quotationId) {
        Quotation quotation = requireQuote(companyId, quotationId);
        if (quotation.status() != QuotationStatus.DRAFT) {
            throw new ConflictException("Quotation is not editable");
        }
        return quotation;
    }

    private User requireSalesUser(long companyId, long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Sales user not found"));
        if (user.companyId() == null || user.companyId() != companyId || !user.active() || !isSalesRole(user.role())) {
            throw new BadRequestException("Assignee must be an active salesperson in this company");
        }
        return user;
    }

    private static boolean isSalesRole(UserRole role) {
        return role == UserRole.SALES_REP || role == UserRole.SALES_MANAGER || role == UserRole.FINANCE_OPS;
    }

    private CustomerTier requireTier(long companyId, long tierId) {
        return tierRepository
                .findById(tierId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
    }

    private ProductVariant requireVariant(long companyId, long productId, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant = variantRepository
                .findById(variantId, companyId)
                .orElseThrow(() -> new NotFoundException("Variant not found"));
        if (variant.productId() != productId) {
            throw new BadRequestException("Variant does not belong to product");
        }
        return variant;
    }

    private static void validateDiscount(BigDecimal discountPercent) {
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Discount must be between 0 and 100");
        }
    }
}

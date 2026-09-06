package com.dealflow360.quoterequest.service;

import com.dealflow360.auth.model.User;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductCategory;
import com.dealflow360.catalog.repository.ProductCategoryRepository;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.policy.model.DiscountPolicy;
import com.dealflow360.pricing.model.CustomerTier;
import com.dealflow360.pricing.model.PriceList;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.quotation.dto.AddQuotationLineRequest;
import com.dealflow360.quotation.dto.CreateQuotationRequest;
import com.dealflow360.quotation.dto.QuotationResponse;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.repository.QuotationRepository;
import com.dealflow360.quotation.service.QuotePricingService;
import com.dealflow360.quotation.service.QuotationService;
import com.dealflow360.quotation.service.RecommendationService;
import com.dealflow360.quotation.service.RiskEngine;
import com.dealflow360.quoterequest.dto.CreateQuoteRequestBody;
import com.dealflow360.quoterequest.dto.CustomerRecommendationResponse;
import com.dealflow360.quoterequest.dto.PatchQuoteRequestBody;
import com.dealflow360.quoterequest.dto.PatchQuoteRequestLineBody;
import com.dealflow360.quoterequest.dto.QuoteRequestLineBody;
import com.dealflow360.quoterequest.dto.QuoteRequestLineResponse;
import com.dealflow360.quoterequest.dto.QuoteRequestResponse;
import com.dealflow360.quoterequest.model.CompanyCustomer;
import com.dealflow360.quoterequest.model.QuoteRequest;
import com.dealflow360.quoterequest.model.QuoteRequestLine;
import com.dealflow360.quoterequest.model.QuoteRequestStatus;
import com.dealflow360.quoterequest.repository.CompanyCustomerRepository;
import com.dealflow360.quoterequest.repository.QuoteRequestLineRepository;
import com.dealflow360.quoterequest.repository.QuoteRequestRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteRequestService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final QuoteRequestRepository quoteRequestRepository;
    private final QuoteRequestLineRepository lineRepository;
    private final CompanyCustomerRepository companyCustomerRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final QuotationService quotationService;
    private final QuotationRepository quotationRepository;
    private final QuotePricingService quotePricingService;
    private final RiskEngine riskEngine;
    private final RecommendationService recommendationService;

    public QuoteRequestService(
            QuoteRequestRepository quoteRequestRepository,
            QuoteRequestLineRepository lineRepository,
            CompanyCustomerRepository companyCustomerRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            ProductCategoryRepository categoryRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            CustomerTierRepository customerTierRepository,
            QuotationService quotationService,
            QuotationRepository quotationRepository,
            QuotePricingService quotePricingService,
            RiskEngine riskEngine,
            RecommendationService recommendationService) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.lineRepository = lineRepository;
        this.companyCustomerRepository = companyCustomerRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.customerTierRepository = customerTierRepository;
        this.quotationService = quotationService;
        this.quotationRepository = quotationRepository;
        this.quotePricingService = quotePricingService;
        this.riskEngine = riskEngine;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public QuoteRequestResponse create(long customerUserId, CreateQuoteRequestBody body) {
        Company seller = requireActiveSeller(body.sellerCompanyId());
        return quoteRequestRepository
                .findDraft(customerUserId, seller.id())
                .map(existing -> toResponse(existing))
                .orElseGet(() -> toResponse(quoteRequestRepository.insert(
                        customerUserId, seller.id(), quoteRequestRepository.nextRequestNumber())));
    }

    public List<QuoteRequestResponse> listForCustomer(long customerUserId) {
        List<QuoteRequestResponse> rows = new ArrayList<>();
        for (QuoteRequest request : quoteRequestRepository.findByCustomer(customerUserId)) {
            rows.add(toResponse(request));
        }
        return rows;
    }

    public QuoteRequestResponse getForCustomer(long customerUserId, long requestId) {
        return toResponse(requireOwned(customerUserId, requestId));
    }

    public List<CustomerRecommendationResponse> recommendations(long customerUserId, long requestId) {
        QuoteRequest request = requireOwned(customerUserId, requestId);
        List<Long> productIds = new ArrayList<>();
        for (QuoteRequestLine line : lineRepository.findByRequest(request.id())) {
            productIds.add(line.productId());
        }
        CustomerTier tier = standingTier(request.sellerCompanyId(), customerUserId);
        Long priceListId = null;
        try {
            priceListId = quotePricingService.requirePriceList(request.sellerCompanyId(), tier.id()).id();
        } catch (RuntimeException ignored) {
            priceListId = null;
        }
        return recommendationService.recommendForCart(request.sellerCompanyId(), priceListId, productIds);
    }

    @Transactional
    public QuoteRequestResponse patch(long customerUserId, long requestId, PatchQuoteRequestBody body) {
        QuoteRequest request = requireDraft(customerUserId, requestId);
        validateExpectedDiscount(body.expectedDiscountPercent());
        boolean updated = quoteRequestRepository.patchDraft(
                request.id(),
                customerUserId,
                body.requestedDeliveryDate(),
                body.expectedDiscountPercent(),
                body.notes());
        if (!updated) {
            throw new ConflictException("Only a draft request can be edited");
        }
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse addLine(long customerUserId, long requestId, QuoteRequestLineBody body) {
        QuoteRequest request = requireDraft(customerUserId, requestId);
        Product product = requireProductForSeller(request.sellerCompanyId(), body.productId());
        CustomerTier tier = standingTier(request.sellerCompanyId(), customerUserId);
        BigDecimal available = riskEngine.allowedDiscount(
                request.sellerCompanyId(), tier, product.categoryId(), riskEngine.policies(request.sellerCompanyId()));
        BigDecimal expected = body.expectedDiscountPercent() == null ? available : body.expectedDiscountPercent();
        validateExpectedDiscount(expected);
        lineRepository.insert(request.id(), body.productId(), body.quantity(), body.notes(), expected);
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse updateLine(
            long customerUserId, long requestId, long lineId, PatchQuoteRequestLineBody body) {
        QuoteRequest request = requireDraft(customerUserId, requestId);
        QuoteRequestLine existing = lineRepository
                .findById(lineId, request.id())
                .orElseThrow(() -> new NotFoundException("Request line not found"));
        BigDecimal quantity = body.quantity() == null ? existing.quantity() : body.quantity();
        String notes = body.notes() == null ? existing.notes() : body.notes();
        BigDecimal expected =
                body.expectedDiscountPercent() == null ? existing.expectedDiscountPercent() : body.expectedDiscountPercent();
        validateExpectedDiscount(expected);
        if (!lineRepository.update(lineId, request.id(), quantity, notes, expected)) {
            throw new NotFoundException("Request line not found");
        }
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse deleteLine(long customerUserId, long requestId, long lineId) {
        requireDraft(customerUserId, requestId);
        if (!lineRepository.delete(lineId, requestId)) {
            throw new NotFoundException("Request line not found");
        }
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse submit(long customerUserId, long requestId) {
        QuoteRequest request = requireDraft(customerUserId, requestId);
        requireActiveSeller(request.sellerCompanyId());
        List<QuoteRequestLine> lines = lineRepository.findByRequest(request.id());
        if (lines.isEmpty()) {
            throw new BadRequestException("Add at least one product before submitting");
        }
        for (QuoteRequestLine line : lines) {
            if (line.quantity() == null || line.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }
            requireProductForSeller(request.sellerCompanyId(), line.productId());
        }
        ensureSellerCustomer(request.sellerCompanyId(), customerUserId);
        quoteRequestRepository.updateStatus(request.id(), QuoteRequestStatus.SUBMITTED, Instant.now(), null);
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse cancel(long customerUserId, long requestId) {
        QuoteRequest request = requireOwned(customerUserId, requestId);
        if (request.quotationId() != null) {
            throw new ConflictException("This request can no longer be cancelled");
        }
        if (request.status() != QuoteRequestStatus.DRAFT
                && request.status() != QuoteRequestStatus.SUBMITTED
                && request.status() != QuoteRequestStatus.UNDER_REVIEW) {
            throw new ConflictException("This request can no longer be cancelled");
        }
        quoteRequestRepository.updateStatus(request.id(), QuoteRequestStatus.CANCELLED, request.submittedAt(), null);
        return toResponse(requireOwned(customerUserId, requestId));
    }

    @Transactional
    public QuoteRequestResponse withdraw(long customerUserId, long requestId) {
        QuoteRequest request = requireOwned(customerUserId, requestId);
        if (request.quotationId() != null) {
            throw new ConflictException("The seller already opened a quotation");
        }
        if (request.status() != QuoteRequestStatus.SUBMITTED && request.status() != QuoteRequestStatus.UNDER_REVIEW) {
            throw new ConflictException("Only a submitted request can be pulled back to draft");
        }
        quoteRequestRepository.updateStatus(request.id(), QuoteRequestStatus.DRAFT, null, null);
        return toResponse(requireOwned(customerUserId, requestId));
    }

    public List<QuoteRequestResponse> listForSeller(long sellerCompanyId) {
        List<QuoteRequestResponse> rows = new ArrayList<>();
        for (QuoteRequest request : quoteRequestRepository.findBySeller(sellerCompanyId)) {
            rows.add(toResponse(request));
        }
        return rows;
    }

    @Transactional
    public QuoteRequestResponse getForSeller(long sellerCompanyId, long requestId) {
        QuoteRequest request = requireSellerRequest(sellerCompanyId, requestId);
        if (request.status() == QuoteRequestStatus.SUBMITTED) {
            quoteRequestRepository.updateStatus(
                    request.id(), QuoteRequestStatus.UNDER_REVIEW, request.submittedAt(), request.quotationId());
            request = requireSellerRequest(sellerCompanyId, requestId);
        }
        return toResponse(request);
    }

    @Transactional
    public QuotationResponse convertToQuotation(long sellerCompanyId, long salesRepId, long requestId) {
        QuoteRequest request = requireSellerRequest(sellerCompanyId, requestId);
        if (request.quotationId() != null) {
            return quotationService.get(sellerCompanyId, request.quotationId());
        }
        if (request.status() != QuoteRequestStatus.SUBMITTED && request.status() != QuoteRequestStatus.UNDER_REVIEW) {
            throw new ConflictException("Only a submitted request can become a quotation");
        }
        CompanyCustomer relationship = companyCustomerRepository
                .find(sellerCompanyId, request.customerUserId())
                .orElseThrow(() -> new ConflictException("Seller customer record is missing"));
        QuotationResponse created = quotationService.create(
                sellerCompanyId, salesRepId, new CreateQuotationRequest(relationship.sellerCustomerId()));
        CustomerTier tier = standingTier(request.sellerCompanyId(), request.customerUserId());
        List<DiscountPolicy> policies = riskEngine.policies(request.sellerCompanyId());
        for (QuoteRequestLine line : lineRepository.findByRequest(request.id())) {
            Product product = requireProductForSeller(request.sellerCompanyId(), line.productId());
            BigDecimal available = riskEngine.allowedDiscount(
                    request.sellerCompanyId(), tier, product.categoryId(), policies);
            quotationService.addLine(
                    sellerCompanyId,
                    created.id(),
                    new AddQuotationLineRequest(
                            line.productId(),
                            null,
                            line.quantity(),
                            appliedExpected(
                                    line.expectedDiscountPercent(),
                                    request.expectedDiscountPercent(),
                                    available)));
        }
        quoteRequestRepository.updateStatus(
                request.id(), QuoteRequestStatus.QUOTED, request.submittedAt(), created.id());
        return quotationService.get(sellerCompanyId, created.id());
    }

    private void ensureSellerCustomer(long sellerCompanyId, long customerUserId) {
        if (companyCustomerRepository.find(sellerCompanyId, customerUserId).isPresent()) {
            return;
        }
        User user = userRepository.findById(customerUserId).orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerTier bronze = defaultTier(sellerCompanyId);
        Customer sellerCustomer = customerRepository
                .findByCompanyAndUser(sellerCompanyId, customerUserId)
                .orElseGet(() -> insertSellerCustomer(sellerCompanyId, user, bronze.id()));
        companyCustomerRepository.insert(sellerCompanyId, customerUserId, bronze.id(), sellerCustomer.id());
    }

    private Customer insertSellerCustomer(long sellerCompanyId, User user, long tierId) {
        try {
            return customerRepository.insert(sellerCompanyId, user.name(), tierId, user.id(), true);
        } catch (ConflictException ignored) {
            String fallbackName = user.name() + " (" + user.email() + ")";
            return customerRepository.insert(sellerCompanyId, fallbackName, tierId, user.id(), true);
        }
    }

    private CustomerTier defaultTier(long sellerCompanyId) {
        List<CustomerTier> tiers = customerTierRepository.findByCompany(sellerCompanyId);
        return tiers.stream()
                .filter(tier -> "Bronze".equalsIgnoreCase(tier.name()) && tier.active())
                .findFirst()
                .or(() -> tiers.stream().filter(CustomerTier::active).findFirst())
                .orElseThrow(() -> new BadRequestException("This seller has no customer tier"));
    }

    private Product requireProductForSeller(long sellerCompanyId, long productId) {
        Product product = productRepository
                .findById(productId, sellerCompanyId)
                .orElseThrow(() -> new BadRequestException("Product does not belong to this seller"));
        if (!product.active()) {
            throw new BadRequestException("Product is inactive");
        }
        return product;
    }

    private Company requireActiveSeller(long sellerCompanyId) {
        Company company =
                companyRepository.findById(sellerCompanyId).orElseThrow(() -> new NotFoundException("Company not found"));
        if (!company.active()) {
            throw new BadRequestException("Seller is inactive");
        }
        return company;
    }

    private QuoteRequest requireOwned(long customerUserId, long requestId) {
        QuoteRequest request =
                quoteRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Request not found"));
        if (request.customerUserId() != customerUserId) {
            throw new NotFoundException("Request not found");
        }
        return request;
    }

    private QuoteRequest requireDraft(long customerUserId, long requestId) {
        QuoteRequest request = requireOwned(customerUserId, requestId);
        if (request.status() != QuoteRequestStatus.DRAFT) {
            throw new ConflictException("Only a draft request can be edited");
        }
        return request;
    }

    private QuoteRequest requireSellerRequest(long sellerCompanyId, long requestId) {
        QuoteRequest request =
                quoteRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Request not found"));
        if (request.sellerCompanyId() != sellerCompanyId || request.status() == QuoteRequestStatus.DRAFT) {
            throw new NotFoundException("Request not found");
        }
        return request;
    }

    private QuoteRequestResponse toResponse(QuoteRequest request) {
        Company seller = companyRepository.findById(request.sellerCompanyId()).orElse(null);
        User customer = userRepository.findById(request.customerUserId()).orElse(null);
        CustomerTier tier = standingTier(request.sellerCompanyId(), request.customerUserId());
        List<DiscountPolicy> policies = riskEngine.policies(request.sellerCompanyId());
        List<QuoteRequestLineResponse> lines = new ArrayList<>();
        BigDecimal catalogMrpTotal = BigDecimal.ZERO;
        BigDecimal indicativeTotal = BigDecimal.ZERO;
        BigDecimal expectedTotal = BigDecimal.ZERO;
        for (QuoteRequestLine line : lineRepository.findByRequest(request.id())) {
            QuoteRequestLineResponse mapped =
                    toLineResponse(request.sellerCompanyId(), line, tier, policies, request.expectedDiscountPercent());
            lines.add(mapped);
            catalogMrpTotal = catalogMrpTotal.add(mapped.lineMrp());
            indicativeTotal = indicativeTotal.add(mapped.indicativeLineTotal());
            expectedTotal = expectedTotal.add(mapped.expectedLineTotal());
        }
        Quotation linked = request.quotationId() == null
                ? null
                : quotationRepository.findById(request.quotationId(), request.sellerCompanyId()).orElse(null);
        return new QuoteRequestResponse(
                request.id(),
                request.requestNumber(),
                request.sellerCompanyId(),
                seller == null ? "" : seller.name(),
                request.customerUserId(),
                customer == null ? "" : customer.name(),
                request.status(),
                statusLabel(request.status()),
                request.requestedDeliveryDate(),
                request.expectedDiscountPercent(),
                request.notes(),
                request.quotationId(),
                request.createdAt(),
                request.updatedAt(),
                request.submittedAt(),
                tier.name(),
                money(catalogMrpTotal),
                money(indicativeTotal),
                money(expectedTotal),
                linked == null ? null : linked.status(),
                linked == null ? null : linked.totalAmount(),
                lines);
    }

    private QuoteRequestLineResponse toLineResponse(
            long sellerCompanyId,
            QuoteRequestLine line,
            CustomerTier tier,
            List<DiscountPolicy> policies,
            BigDecimal overallExpected) {
        Product product = productRepository.findById(line.productId(), sellerCompanyId).orElse(null);
        if (product == null) {
            return new QuoteRequestLineResponse(
                    line.id(),
                    line.productId(),
                    "Product",
                    "Catalog",
                    "unit",
                    BillingType.ONE_TIME,
                    line.quantity(),
                    line.notes(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }
        String categoryName = categoryRepository
                .findById(product.categoryId(), sellerCompanyId)
                .map(ProductCategory::name)
                .orElse("Catalog");
        BigDecimal standing = riskEngine.standingDiscount(tier, policies);
        BigDecimal category = riskEngine.categoryDiscount(product.categoryId(), policies);
        BigDecimal available = riskEngine.allowedDiscount(sellerCompanyId, tier, product.categoryId(), policies);
        BigDecimal unitPrice = listUnitPrice(sellerCompanyId, tier, product);
        BigDecimal quantity = line.quantity() == null ? BigDecimal.ZERO : line.quantity();
        BigDecimal lineMrp = money(product.basePrice().multiply(quantity));
        BigDecimal storedExpected = line.expectedDiscountPercent() == null ? available : line.expectedDiscountPercent();
        boolean independent = isIndependentExpected(line.expectedDiscountPercent(), available);
        BigDecimal applied = appliedExpected(line.expectedDiscountPercent(), overallExpected, available);
        BigDecimal indicativeUnit = afterDiscount(unitPrice, available);
        BigDecimal indicativeLine = money(indicativeUnit.multiply(quantity));
        BigDecimal expectedUnit = afterDiscount(unitPrice, applied);
        BigDecimal expectedLine = money(expectedUnit.multiply(quantity));
        return new QuoteRequestLineResponse(
                line.id(),
                line.productId(),
                product.name(),
                categoryName,
                product.unit(),
                product.billingType(),
                line.quantity(),
                line.notes(),
                money(product.basePrice()),
                lineMrp,
                category,
                standing,
                available,
                storedExpected,
                independent,
                applied,
                indicativeUnit,
                indicativeLine,
                expectedUnit,
                expectedLine);
    }

    private BigDecimal listUnitPrice(long sellerCompanyId, CustomerTier tier, Product product) {
        try {
            PriceList priceList = quotePricingService.requirePriceList(sellerCompanyId, tier.id());
            return quotePricingService.resolveUnitPrice(sellerCompanyId, priceList.id(), product, null);
        } catch (RuntimeException ignored) {
            return money(product.basePrice());
        }
    }

    private CustomerTier standingTier(long sellerCompanyId, long customerUserId) {
        return companyCustomerRepository
                .find(sellerCompanyId, customerUserId)
                .flatMap(row -> customerTierRepository.findById(row.customerTierId(), sellerCompanyId))
                .orElseGet(() -> defaultTier(sellerCompanyId));
    }

    private static BigDecimal afterDiscount(BigDecimal unitPrice, BigDecimal percent) {
        BigDecimal factor = BigDecimal.ONE.subtract(percent.divide(HUNDRED, 6, ROUNDING));
        return money(unitPrice.multiply(factor));
    }

    public static BigDecimal appliedExpected(BigDecimal lineExpected, BigDecimal overallExpected, BigDecimal available) {
        if (isIndependentExpected(lineExpected, available)) {
            return lineExpected;
        }
        if (overallExpected != null) {
            return overallExpected;
        }
        if (lineExpected != null) {
            return lineExpected;
        }
        return available;
    }

    public static boolean isIndependentExpected(BigDecimal lineExpected, BigDecimal available) {
        return lineExpected != null && available != null && lineExpected.compareTo(available) != 0;
    }

    private static void validateExpectedDiscount(BigDecimal expected) {
        if (expected == null) {
            return;
        }
        if (expected.compareTo(BigDecimal.ZERO) < 0 || expected.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Expected discount must be between 0 and 100");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    static String statusLabel(QuoteRequestStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Seller Reviewing";
            case QUOTED -> "Quotation Ready";
            case CANCELLED -> "Cancelled";
            case CLOSED -> "Closed";
        };
    }
}

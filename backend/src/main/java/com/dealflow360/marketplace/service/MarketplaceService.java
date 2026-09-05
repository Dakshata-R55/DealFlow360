package com.dealflow360.marketplace.service;

import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductCategory;
import com.dealflow360.catalog.repository.ProductCategoryRepository;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.marketplace.dto.PublicProductResponse;
import com.dealflow360.marketplace.dto.SellerCompanyResponse;
import com.dealflow360.policy.model.DiscountPolicy;
import com.dealflow360.policy.repository.DiscountPolicyRepository;
import com.dealflow360.quotation.service.RiskEngine;
import com.dealflow360.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceService {

    private final CompanyRepository companyRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final RiskEngine riskEngine;

    public MarketplaceService(
            CompanyRepository companyRepository,
            ProductCategoryRepository categoryRepository,
            ProductRepository productRepository,
            DiscountPolicyRepository discountPolicyRepository,
            RiskEngine riskEngine) {
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.discountPolicyRepository = discountPolicyRepository;
        this.riskEngine = riskEngine;
    }

    public List<SellerCompanyResponse> listCompanies(String query) {
        return companyRepository.findActive(query).stream().map(this::toCompany).toList();
    }

    public SellerCompanyResponse getCompany(long companyId) {
        return toCompany(requireActiveCompany(companyId));
    }

    public List<PublicProductResponse> listProducts(long companyId) {
        requireActiveCompany(companyId);
        List<DiscountPolicy> policies = discountPolicyRepository.findByCompany(companyId);
        return productRepository.findByCompany(companyId).stream()
                .filter(Product::active)
                .map(product -> toProduct(companyId, product, policies))
                .toList();
    }

    public PublicProductResponse getProduct(long companyId, long productId) {
        requireActiveCompany(companyId);
        Product product = productRepository
                .findById(productId, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.active()) {
            throw new NotFoundException("Product not found");
        }
        return toProduct(companyId, product, discountPolicyRepository.findByCompany(companyId));
    }

    private Company requireActiveCompany(long companyId) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new NotFoundException("Company not found"));
        if (!company.active()) {
            throw new NotFoundException("Company not found");
        }
        return company;
    }

    private SellerCompanyResponse toCompany(Company company) {
        List<String> categories = categoryRepository.findByCompany(company.id()).stream()
                .filter(ProductCategory::active)
                .map(ProductCategory::name)
                .toList();
        return new SellerCompanyResponse(company.id(), company.name(), company.code(), company.description(), categories);
    }

    private PublicProductResponse toProduct(long companyId, Product product, List<DiscountPolicy> policies) {
        String categoryName = categoryRepository
                .findById(product.categoryId(), companyId)
                .map(ProductCategory::name)
                .orElse("Catalog");
        BigDecimal categoryDiscount = riskEngine.categoryDiscount(product.categoryId(), policies);
        return new PublicProductResponse(
                product.id(),
                product.name(),
                categoryName,
                product.description(),
                product.unit(),
                product.basePrice(),
                categoryDiscount,
                product.billingType(),
                product.active());
    }
}

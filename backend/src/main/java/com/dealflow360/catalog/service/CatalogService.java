package com.dealflow360.catalog.service;

import com.dealflow360.catalog.dto.CategoryRequest;
import com.dealflow360.catalog.dto.CategoryResponse;
import com.dealflow360.catalog.dto.CreateProductRequest;
import com.dealflow360.catalog.dto.PatchProductRequest;
import com.dealflow360.catalog.dto.ProductResponse;
import com.dealflow360.catalog.dto.ProductVariantResponse;
import com.dealflow360.catalog.dto.VariantRequest;
import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.model.ProductCategory;
import com.dealflow360.catalog.repository.ProductCategoryRepository;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.catalog.repository.ProductVariantRepository;
import com.dealflow360.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public CatalogService(
            ProductCategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    public List<CategoryResponse> listCategories(long companyId) {
        return categoryRepository.findByCompany(companyId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(long companyId, CategoryRequest request) {
        ProductCategory category =
                categoryRepository.insert(companyId, request.name().trim(), request.active() == null || request.active());
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateCategory(long companyId, long id, CategoryRequest request) {
        ProductCategory existing = categoryRepository
                .findById(id, companyId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        boolean active = request.active() == null ? existing.active() : request.active();
        return CategoryResponse.from(categoryRepository
                .update(id, companyId, request.name().trim(), active)
                .orElseThrow(() -> new NotFoundException("Category not found")));
    }

    public List<ProductResponse> listProducts(long companyId) {
        return productRepository.findByCompany(companyId).stream()
                .map(product -> toProductResponse(companyId, product))
                .toList();
    }

    public ProductResponse getProduct(long companyId, long id) {
        Product product = productRepository
                .findById(id, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return toProductResponse(companyId, product);
    }

    @Transactional
    public ProductResponse createProduct(long companyId, CreateProductRequest request) {
        requireCategory(companyId, request.categoryId());
        Product product = productRepository.insert(
                companyId,
                request.categoryId(),
                request.name().trim(),
                blankToEmpty(request.description()),
                request.unit().trim(),
                request.basePrice(),
                request.costPrice(),
                request.taxPercent(),
                request.billingType(),
                request.active() == null || request.active());
        return toProductResponse(companyId, product);
    }

    @Transactional
    public ProductResponse updateProduct(long companyId, long id, PatchProductRequest request) {
        Product existing = productRepository
                .findById(id, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        long categoryId = request.categoryId() == null ? existing.categoryId() : request.categoryId();
        requireCategory(companyId, categoryId);
        Product updated = new Product(
                existing.id(),
                existing.companyId(),
                categoryId,
                request.name() == null ? existing.name() : request.name().trim(),
                request.description() == null ? existing.description() : blankToEmpty(request.description()),
                request.unit() == null ? existing.unit() : request.unit().trim(),
                request.basePrice() == null ? existing.basePrice() : request.basePrice(),
                request.costPrice() == null ? existing.costPrice() : request.costPrice(),
                request.taxPercent() == null ? existing.taxPercent() : request.taxPercent(),
                request.billingType() == null ? existing.billingType() : request.billingType(),
                request.active() == null ? existing.active() : request.active(),
                existing.createdAt(),
                existing.updatedAt());
        Product saved = productRepository
                .update(updated)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return toProductResponse(companyId, saved);
    }

    @Transactional
    public ProductVariantResponse createVariant(long companyId, long productId, VariantRequest request) {
        requireProduct(companyId, productId);
        return ProductVariantResponse.from(variantRepository.insert(
                companyId,
                productId,
                request.attributeName().trim(),
                request.attributeValue().trim(),
                request.extraPrice()));
    }

    @Transactional
    public ProductVariantResponse updateVariant(
            long companyId, long productId, long variantId, VariantRequest request) {
        requireProduct(companyId, productId);
        return ProductVariantResponse.from(variantRepository
                .update(
                        companyId,
                        productId,
                        variantId,
                        request.attributeName().trim(),
                        request.attributeValue().trim(),
                        request.extraPrice())
                .orElseThrow(() -> new NotFoundException("Variant not found")));
    }

    private ProductResponse toProductResponse(long companyId, Product product) {
        List<ProductVariantResponse> variants = variantRepository.findByProduct(product.id(), companyId).stream()
                .map(ProductVariantResponse::from)
                .toList();
        return ProductResponse.from(product, variants);
    }

    private void requireCategory(long companyId, long categoryId) {
        categoryRepository
                .findById(categoryId, companyId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void requireProduct(long companyId, long productId) {
        productRepository
                .findById(productId, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

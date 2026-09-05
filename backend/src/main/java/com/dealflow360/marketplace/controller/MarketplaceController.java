package com.dealflow360.marketplace.controller;

import com.dealflow360.marketplace.dto.PublicProductResponse;
import com.dealflow360.marketplace.dto.SellerCompanyResponse;
import com.dealflow360.marketplace.service.MarketplaceService;
import com.dealflow360.shared.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/companies")
@PreAuthorize("hasAuthority('CUSTOMER')")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerCompanyResponse>>> list(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, marketplaceService.listCompanies(query)));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<SellerCompanyResponse>> get(@PathVariable long companyId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, marketplaceService.getCompany(companyId)));
    }

    @GetMapping("/{companyId}/products")
    public ResponseEntity<ApiResponse<List<PublicProductResponse>>> products(@PathVariable long companyId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, marketplaceService.listProducts(companyId)));
    }

    @GetMapping("/{companyId}/products/{productId}")
    public ResponseEntity<ApiResponse<PublicProductResponse>> product(
            @PathVariable long companyId, @PathVariable long productId) {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, marketplaceService.getProduct(companyId, productId)));
    }
}

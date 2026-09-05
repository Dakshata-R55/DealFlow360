package com.dealflow360.policy.service;

import com.dealflow360.catalog.repository.ProductCategoryRepository;
import com.dealflow360.policy.dto.ApprovalPolicyReplaceRequest;
import com.dealflow360.policy.dto.ApprovalPolicyResponse;
import com.dealflow360.policy.dto.ApprovalPolicyRowRequest;
import com.dealflow360.policy.dto.DiscountPolicyReplaceRequest;
import com.dealflow360.policy.dto.DiscountPolicyResponse;
import com.dealflow360.policy.dto.DiscountPolicyRowRequest;
import com.dealflow360.policy.repository.ApprovalPolicyRepository;
import com.dealflow360.policy.repository.DiscountPolicyRepository;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final DiscountPolicyRepository discountPolicyRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;
    private final CustomerTierRepository tierRepository;
    private final ProductCategoryRepository categoryRepository;

    public PolicyService(
            DiscountPolicyRepository discountPolicyRepository,
            ApprovalPolicyRepository approvalPolicyRepository,
            CustomerTierRepository tierRepository,
            ProductCategoryRepository categoryRepository) {
        this.discountPolicyRepository = discountPolicyRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
        this.tierRepository = tierRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<DiscountPolicyResponse> listDiscountPolicies(long companyId) {
        return discountPolicyRepository.findByCompany(companyId).stream()
                .map(DiscountPolicyResponse::from)
                .toList();
    }

    @Transactional
    public List<DiscountPolicyResponse> replaceDiscountPolicies(long companyId, DiscountPolicyReplaceRequest request) {
        for (DiscountPolicyRowRequest row : request.policies()) {
            validateXor(row);
            if (row.customerTierId() != null) {
                tierRepository
                        .findById(row.customerTierId(), companyId)
                        .orElseThrow(() -> new NotFoundException("Customer tier not found"));
            }
            if (row.categoryId() != null) {
                categoryRepository
                        .findById(row.categoryId(), companyId)
                        .orElseThrow(() -> new NotFoundException("Category not found"));
            }
        }
        discountPolicyRepository.deleteByCompany(companyId);
        List<DiscountPolicyResponse> saved = new ArrayList<>();
        for (DiscountPolicyRowRequest row : request.policies()) {
            saved.add(DiscountPolicyResponse.from(discountPolicyRepository.insert(
                    companyId, row.customerTierId(), row.categoryId(), row.maxDiscountPct())));
        }
        return saved;
    }

    public List<ApprovalPolicyResponse> listApprovalPolicies(long companyId) {
        return approvalPolicyRepository.findByCompany(companyId).stream()
                .map(ApprovalPolicyResponse::from)
                .toList();
    }

    @Transactional
    public List<ApprovalPolicyResponse> replaceApprovalPolicies(long companyId, ApprovalPolicyReplaceRequest request) {
        approvalPolicyRepository.deleteByCompany(companyId);
        List<ApprovalPolicyResponse> saved = new ArrayList<>();
        for (ApprovalPolicyRowRequest row : request.policies()) {
            saved.add(ApprovalPolicyResponse.from(approvalPolicyRepository.insert(
                    companyId,
                    row.riskLevel(),
                    row.minScore(),
                    row.maxScore(),
                    row.requiresManager(),
                    row.requiresFinance(),
                    row.hardLineExcessThreshold())));
        }
        return saved;
    }

    private static void validateXor(DiscountPolicyRowRequest row) {
        boolean hasTier = row.customerTierId() != null;
        boolean hasCategory = row.categoryId() != null;
        if (hasTier == hasCategory) {
            throw new BadRequestException("Discount policy must set either customerTierId or categoryId");
        }
    }
}

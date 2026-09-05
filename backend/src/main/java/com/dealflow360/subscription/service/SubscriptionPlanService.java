package com.dealflow360.subscription.service;

import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.subscription.dto.SubscriptionPlanRequest;
import com.dealflow360.subscription.dto.SubscriptionPlanResponse;
import com.dealflow360.subscription.model.SubscriptionPlan;
import com.dealflow360.subscription.repository.SubscriptionPlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<SubscriptionPlanResponse> list(long companyId) {
        return planRepository.findByCompany(companyId).stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    @Transactional
    public SubscriptionPlanResponse create(long companyId, SubscriptionPlanRequest request) {
        return SubscriptionPlanResponse.from(planRepository.insert(
                companyId,
                request.name().trim(),
                request.cycle(),
                request.prorationRule(),
                request.cancellationRule(),
                request.active() == null || request.active()));
    }

    @Transactional
    public SubscriptionPlanResponse update(long companyId, long id, SubscriptionPlanRequest request) {
        SubscriptionPlan existing = planRepository
                .findById(id, companyId)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found"));
        SubscriptionPlan updated = new SubscriptionPlan(
                existing.id(),
                existing.companyId(),
                request.name().trim(),
                request.cycle(),
                request.prorationRule(),
                request.cancellationRule(),
                request.active() == null ? existing.active() : request.active(),
                existing.createdAt(),
                existing.updatedAt());
        return SubscriptionPlanResponse.from(planRepository
                .update(updated)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found")));
    }
}

package com.dealflow360.customer.service;

import com.dealflow360.customer.dto.CustomerResponse;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.pricing.repository.CustomerTierRepository;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.standing.service.StandingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final StandingService standingService;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerTierRepository customerTierRepository,
            StandingService standingService) {
        this.customerRepository = customerRepository;
        this.customerTierRepository = customerTierRepository;
        this.standingService = standingService;
    }

    public List<CustomerResponse> list(long companyId) {
        return customerRepository.findByCompany(companyId).stream()
                .map(customer -> toResponse(companyId, customer))
                .toList();
    }

    public Customer require(long companyId, long customerId) {
        return customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Transactional
    public CustomerResponse assignTier(long companyId, long customerId, long customerTierId) {
        Customer updated = standingService.applyTier(companyId, customerId, customerTierId);
        return toResponse(companyId, updated);
    }

    private CustomerResponse toResponse(long companyId, Customer customer) {
        String name = customerTierRepository
                .findById(customer.customerTierId(), companyId)
                .map(tier -> tier.name())
                .orElse("");
        return CustomerResponse.from(customer, name);
    }
}

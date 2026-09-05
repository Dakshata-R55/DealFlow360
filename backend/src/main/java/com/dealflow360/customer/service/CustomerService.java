package com.dealflow360.customer.service;

import com.dealflow360.customer.dto.CustomerResponse;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> list(long companyId) {
        return customerRepository.findByCompany(companyId).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    public Customer require(long companyId, long customerId) {
        return customerRepository
                .findById(customerId, companyId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }
}

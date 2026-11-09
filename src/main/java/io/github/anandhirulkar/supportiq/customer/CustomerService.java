package io.github.anandhirulkar.supportiq.customer;

import io.github.anandhirulkar.supportiq.customer.dto.CustomerDtos.CreateCustomerRequest;
import io.github.anandhirulkar.supportiq.customer.dto.CustomerDtos.CustomerResponse;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.shared.exception.DuplicateResourceException;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customers.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Customer", "email", request.email());
        }
        Customer customer = Customer.register(
                request.name().trim(), request.email().trim().toLowerCase(), request.company(), request.tier());
        return CustomerResponse.from(customers.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return CustomerResponse.from(customers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String query, Pageable pageable) {
        var page = StringUtils.hasText(query)
                ? customers.findByNameContainingIgnoreCaseOrCompanyContainingIgnoreCase(query, query, pageable)
                : customers.findAll(pageable);
        return PageResponse.from(page, CustomerResponse::from);
    }
}

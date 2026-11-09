package io.github.anandhirulkar.supportiq.customer;

import io.github.anandhirulkar.supportiq.customer.dto.CustomerDtos.CreateCustomerRequest;
import io.github.anandhirulkar.supportiq.customer.dto.CustomerDtos.CustomerResponse;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer records stored in MySQL")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a customer by id")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @GetMapping
    @Operation(summary = "Search customers")
    public PageResponse<CustomerResponse> search(
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return customerService.search(q, pageable);
    }
}

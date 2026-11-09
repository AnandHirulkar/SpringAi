package io.github.anandhirulkar.supportiq.customer.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.github.anandhirulkar.supportiq.customer.Customer;
import io.github.anandhirulkar.supportiq.customer.CustomerTier;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CreateCustomerRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @Size(max = 120) String company,
            CustomerTier tier) {
    }

    public record CustomerResponse(
            Long id, String name, String email, String company, CustomerTier tier, Instant createdAt) {

        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getCompany(),
                    customer.getTier(),
                    customer.getCreatedAt());
        }
    }
}

package io.github.anandhirulkar.supportiq.customer;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<Customer> findByNameContainingIgnoreCaseOrCompanyContainingIgnoreCase(
            String name, String company, Pageable pageable);
}

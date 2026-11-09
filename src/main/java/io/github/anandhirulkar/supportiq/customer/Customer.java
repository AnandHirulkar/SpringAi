package io.github.anandhirulkar.supportiq.customer;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Aggregate root for the customer bounded context. */
@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customers_email", columnNames = "email"))
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(length = 120)
    private String company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerTier tier = CustomerTier.STANDARD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {
        // for JPA
    }

    private Customer(String name, String email, String company, CustomerTier tier) {
        this.name = name;
        this.email = email;
        this.company = company;
        this.tier = tier == null ? CustomerTier.STANDARD : tier;
        this.createdAt = Instant.now();
    }

    public static Customer register(String name, String email, String company, CustomerTier tier) {
        return new Customer(name, email, company, tier);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCompany() {
        return company;
    }

    public CustomerTier getTier() {
        return tier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.challengebank.customer.model.entity;

import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.model.enums.DocumentType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customers_document", columnNames = {"document_type", "document_id"}),
        @UniqueConstraint(name = "uk_customers_email", columnNames = {"email"})
})
public class Customer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id", updatable = false, nullable = false)
    public UUID customerId;

    @Column(name = "first_name", nullable = false, length = 100)
    public String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    public String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    public DocumentType documentType;

    @Column(name = "document_id", nullable = false, length = 20)
    public String documentId;

    @Column(name = "email", nullable = false)
    public String email;

    @Column(name = "phone", length = 20)
    public String phone;

    @Column(name = "date_of_birth")
    public LocalDate dateOfBirth;

    @Column(name = "address", length = 500)
    public String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    public CustomerStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}

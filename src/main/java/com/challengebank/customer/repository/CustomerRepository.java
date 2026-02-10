package com.challengebank.customer.repository;

import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.model.enums.DocumentType;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<Customer, UUID> {

    public Optional<Customer> findByDocumentId(String documentId) {
        return find("documentId", documentId).firstResultOptional();
    }

    public Optional<Customer> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<Customer> findByDocumentTypeAndDocumentId(DocumentType documentType, String documentId) {
        return find("documentType = ?1 and documentId = ?2", documentType, documentId).firstResultOptional();
    }

    public PanacheQuery<Customer> findByStatus(CustomerStatus status) {
        return find("status", status);
    }

    public long countByStatus(CustomerStatus status) {
        return count("status", status);
    }

    public boolean existsByDocumentTypeAndDocumentId(DocumentType documentType, String documentId) {
        return count("documentType = ?1 and documentId = ?2", documentType, documentId) > 0;
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}

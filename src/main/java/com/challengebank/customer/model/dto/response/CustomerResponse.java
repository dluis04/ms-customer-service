package com.challengebank.customer.model.dto.response;

import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.model.enums.DocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerResponse {

    public UUID customerId;
    public String firstName;
    public String lastName;
    public DocumentType documentType;
    public String documentId;
    public String email;
    public String phone;
    public LocalDate dateOfBirth;
    public CustomerStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

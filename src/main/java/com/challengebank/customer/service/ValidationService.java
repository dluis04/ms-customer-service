package com.challengebank.customer.service;

import com.challengebank.customer.model.dto.request.ValidateCustomerRequest;
import com.challengebank.customer.model.dto.response.ValidationResponse;
import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ValidationService {

    @Inject
    CustomerRepository customerRepository;

    @Inject
    MeterRegistry meterRegistry;

    Counter validationSuccessCounter;
    Counter validationFailureCounter;

    @PostConstruct
    void initMetrics() {
        validationSuccessCounter = meterRegistry.counter("customer.validation.success");
        validationFailureCounter = meterRegistry.counter("customer.validation.failure");
    }

    public ValidationResponse validateCustomer(ValidateCustomerRequest request) {
        if (request.customerId == null && (request.documentId == null || request.documentId.isBlank())) {
            validationFailureCounter.increment();
            throw new IllegalArgumentException("At least one of customerId or documentId must be provided");
        }

        Optional<Customer> customerOpt;
        if (request.customerId != null) {
            customerOpt = customerRepository.findByIdOptional(request.customerId);
        } else {
            customerOpt = customerRepository.findByDocumentId(request.documentId);
        }

        return buildValidationResponse(customerOpt);
    }

    public ValidationResponse validateCustomerById(UUID customerId) {
        Optional<Customer> customerOpt = customerRepository.findByIdOptional(customerId);
        return buildValidationResponse(customerOpt);
    }

    private ValidationResponse buildValidationResponse(Optional<Customer> customerOpt) {
        ValidationResponse response = new ValidationResponse();
        if (customerOpt.isEmpty()) {
            response.valid = false;
            response.message = "Customer not found";
            validationFailureCounter.increment();
        } else {
            Customer customer = customerOpt.get();
            response.customerId = customer.customerId;
            response.status = customer.status;
            response.valid = customer.status == CustomerStatus.ACTIVE;
            response.message = response.valid
                    ? "Customer is active and valid"
                    : "Customer exists but is not active (status: " + customer.status + ")";
            if (response.valid) {
                validationSuccessCounter.increment();
            } else {
                validationFailureCounter.increment();
            }
        }
        return response;
    }
}

package com.challengebank.customer.service;

import com.challengebank.customer.exception.CustomerNotFoundException;
import com.challengebank.customer.exception.DuplicateCustomerException;
import com.challengebank.customer.mapper.CustomerMapper;
import com.challengebank.customer.model.dto.request.CreateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateStatusRequest;
import com.challengebank.customer.model.dto.response.CustomerPageResponse;
import com.challengebank.customer.model.dto.response.CustomerResponse;
import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository customerRepository;

    @Inject
    CustomerMapper customerMapper;

    @Inject
    MeterRegistry meterRegistry;

    Counter successCounter;
    Counter failureCounter;

    @PostConstruct
    void initMetrics() {
        successCounter = meterRegistry.counter("customer.operations.success");
        failureCounter = meterRegistry.counter("customer.operations.failure");
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByDocumentTypeAndDocumentId(request.documentType, request.documentId)) {
            failureCounter.increment();
            throw new DuplicateCustomerException(
                    "Customer with document " + request.documentType + ":" + request.documentId + " already exists");
        }
        if (customerRepository.existsByEmail(request.email)) {
            failureCounter.increment();
            throw new DuplicateCustomerException("Customer with email " + request.email + " already exists");
        }

        Customer customer = customerMapper.toEntity(request);
        customerRepository.persist(customer);
        successCounter.increment();
        Log.infof("Customer created: %s", customer.customerId);
        return customerMapper.toResponse(customer);
    }

    public CustomerPageResponse getAllCustomers(int page, int size, CustomerStatus status) {
        PanacheQuery<Customer> query;
        if (status != null) {
            query = customerRepository.findByStatus(status);
        } else {
            query = customerRepository.findAll();
        }
        List<Customer> customers = query.page(Page.of(page, size)).list();
        long total = query.count();
        return customerMapper.toPageResponse(customers, page, size, total);
    }

    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findByIdOptional(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        return customerMapper.toResponse(customer);
    }

    public CustomerResponse getCustomerByDocument(String documentId) {
        Customer customer = customerRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with document: " + documentId));
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByIdOptional(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        if (request.email != null && !request.email.equals(customer.email)) {
            if (customerRepository.existsByEmail(request.email)) {
                failureCounter.increment();
                throw new DuplicateCustomerException("Email already in use: " + request.email);
            }
        }

        customerMapper.updateEntity(customer, request);
        customerRepository.persist(customer);
        successCounter.increment();
        Log.infof("Customer updated: %s", customerId);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = customerRepository.findByIdOptional(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        customer.status = CustomerStatus.INACTIVE;
        customerRepository.persist(customer);
        successCounter.increment();
        Log.infof("Customer soft-deleted: %s", customerId);
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(UUID customerId, UpdateStatusRequest request) {
        Customer customer = customerRepository.findByIdOptional(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        customer.status = request.status;
        customerRepository.persist(customer);
        successCounter.increment();
        Log.infof("Customer %s status updated to %s. Reason: %s", customerId, request.status, request.reason);
        return customerMapper.toResponse(customer);
    }
}

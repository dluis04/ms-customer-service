package com.challengebank.customer.service;

import com.challengebank.customer.model.dto.request.ValidateCustomerRequest;
import com.challengebank.customer.model.dto.response.ValidationResponse;
import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    CustomerRepository customerRepository;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter validationSuccessCounter;

    @Mock
    Counter validationFailureCounter;

    @InjectMocks
    ValidationService validationService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("customer.validation.success")).thenReturn(validationSuccessCounter);
        when(meterRegistry.counter("customer.validation.failure")).thenReturn(validationFailureCounter);
        validationService.initMetrics();
    }

    @Test
    void testValidateCustomer_byCustomerId_active() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.ACTIVE;

        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.customerId = customerId;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomer(request);

        assertTrue(response.valid);
        assertEquals(customerId, response.customerId);
        assertEquals(CustomerStatus.ACTIVE, response.status);
        assertEquals("Customer is active and valid", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateCustomer_byCustomerId_inactive() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.INACTIVE;

        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.customerId = customerId;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomer(request);

        assertFalse(response.valid);
        assertEquals(customerId, response.customerId);
        assertEquals(CustomerStatus.INACTIVE, response.status);
        assertTrue(response.message.contains("not active"));
        assertTrue(response.message.contains("INACTIVE"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomer_byDocumentId_found_active() {
        String documentId = "12345678";
        Customer customer = new Customer();
        customer.customerId = UUID.randomUUID();
        customer.status = CustomerStatus.ACTIVE;

        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.documentId = documentId;
        // customerId is null, so it should go to the documentId branch

        when(customerRepository.findByDocumentId(documentId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomer(request);

        assertTrue(response.valid);
        assertEquals(customer.customerId, response.customerId);
        assertEquals(CustomerStatus.ACTIVE, response.status);
        assertEquals("Customer is active and valid", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateCustomer_byDocumentId_notFound() {
        String documentId = "NOTEXIST";

        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.documentId = documentId;

        when(customerRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validateCustomer(request);

        assertFalse(response.valid);
        assertNull(response.customerId);
        assertNull(response.status);
        assertEquals("Customer not found", response.message);
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomer_noIdentifier() {
        ValidateCustomerRequest request = new ValidateCustomerRequest();
        // both customerId and documentId are null

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCustomer(request));
        assertTrue(ex.getMessage().contains("At least one of customerId or documentId must be provided"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomer_blankDocumentId_noCustomerId() {
        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.documentId = "   "; // blank

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateCustomer(request));
        assertTrue(ex.getMessage().contains("At least one of customerId or documentId must be provided"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomerById_active() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.ACTIVE;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomerById(customerId);

        assertTrue(response.valid);
        assertEquals(customerId, response.customerId);
        assertEquals(CustomerStatus.ACTIVE, response.status);
        assertEquals("Customer is active and valid", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateCustomerById_notFound() {
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validateCustomerById(customerId);

        assertFalse(response.valid);
        assertNull(response.customerId);
        assertNull(response.status);
        assertEquals("Customer not found", response.message);
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomerById_suspended() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.SUSPENDED;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomerById(customerId);

        assertFalse(response.valid);
        assertEquals(customerId, response.customerId);
        assertEquals(CustomerStatus.SUSPENDED, response.status);
        assertTrue(response.message.contains("not active"));
        assertTrue(response.message.contains("SUSPENDED"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateCustomerById_pending() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.PENDING;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        ValidationResponse response = validationService.validateCustomerById(customerId);

        assertFalse(response.valid);
        assertEquals(customerId, response.customerId);
        assertEquals(CustomerStatus.PENDING, response.status);
        assertTrue(response.message.contains("not active"));
        assertTrue(response.message.contains("PENDING"));
        verify(validationFailureCounter).increment();
    }
}

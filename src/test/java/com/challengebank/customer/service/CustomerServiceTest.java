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
import com.challengebank.customer.model.enums.DocumentType;
import com.challengebank.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository customerRepository;

    @Mock
    CustomerMapper customerMapper;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter successCounter;

    @Mock
    Counter failureCounter;

    @InjectMocks
    CustomerService customerService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("customer.operations.success")).thenReturn(successCounter);
        when(meterRegistry.counter("customer.operations.failure")).thenReturn(failureCounter);
        customerService.initMetrics();
    }

    @Test
    void testCreateCustomer_success() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.firstName = "John";
        request.lastName = "Doe";
        request.documentType = DocumentType.DNI;
        request.documentId = "12345678";
        request.email = "john@example.com";
        request.phone = "+51999999999";
        request.dateOfBirth = LocalDate.of(1990, 1, 1);
        request.address = "123 Street";

        Customer customer = new Customer();
        customer.customerId = UUID.randomUUID();

        CustomerResponse expectedResponse = new CustomerResponse();
        expectedResponse.customerId = customer.customerId;

        when(customerRepository.existsByDocumentTypeAndDocumentId(DocumentType.DNI, "12345678")).thenReturn(false);
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.createCustomer(request);

        assertEquals(expectedResponse.customerId, result.customerId);
        verify(customerRepository).persist(customer);
        verify(successCounter).increment();
    }

    @Test
    void testCreateCustomer_duplicateDocument() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.documentType = DocumentType.DNI;
        request.documentId = "12345678";
        request.email = "john@example.com";

        when(customerRepository.existsByDocumentTypeAndDocumentId(DocumentType.DNI, "12345678")).thenReturn(true);

        DuplicateCustomerException ex = assertThrows(DuplicateCustomerException.class,
                () -> customerService.createCustomer(request));
        assertTrue(ex.getMessage().contains("DNI"));
        assertTrue(ex.getMessage().contains("12345678"));
        verify(failureCounter).increment();
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    @Test
    void testCreateCustomer_duplicateEmail() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.documentType = DocumentType.DNI;
        request.documentId = "12345678";
        request.email = "john@example.com";

        when(customerRepository.existsByDocumentTypeAndDocumentId(DocumentType.DNI, "12345678")).thenReturn(false);
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        DuplicateCustomerException ex = assertThrows(DuplicateCustomerException.class,
                () -> customerService.createCustomer(request));
        assertTrue(ex.getMessage().contains("john@example.com"));
        verify(failureCounter).increment();
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllCustomers_noFilter() {
        PanacheQuery<Customer> query = mock(PanacheQuery.class);
        Customer customer = new Customer();
        customer.customerId = UUID.randomUUID();
        List<Customer> customers = List.of(customer);
        CustomerPageResponse expectedPage = new CustomerPageResponse();

        when(customerRepository.findAll()).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(customers);
        when(query.count()).thenReturn(1L);
        when(customerMapper.toPageResponse(customers, 0, 10, 1L)).thenReturn(expectedPage);

        CustomerPageResponse result = customerService.getAllCustomers(0, 10, null);

        assertSame(expectedPage, result);
        verify(customerRepository).findAll();
        verify(customerRepository, never()).findByStatus(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllCustomers_withStatusFilter() {
        PanacheQuery<Customer> query = mock(PanacheQuery.class);
        List<Customer> customers = List.of(new Customer());
        CustomerPageResponse expectedPage = new CustomerPageResponse();

        when(customerRepository.findByStatus(CustomerStatus.ACTIVE)).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(customers);
        when(query.count()).thenReturn(5L);
        when(customerMapper.toPageResponse(customers, 0, 10, 5L)).thenReturn(expectedPage);

        CustomerPageResponse result = customerService.getAllCustomers(0, 10, CustomerStatus.ACTIVE);

        assertSame(expectedPage, result);
        verify(customerRepository).findByStatus(CustomerStatus.ACTIVE);
        verify(customerRepository, never()).findAll();
    }

    @Test
    void testGetCustomerById_found() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        CustomerResponse expectedResponse = new CustomerResponse();
        expectedResponse.customerId = customerId;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.getCustomerById(customerId);

        assertEquals(customerId, result.customerId);
    }

    @Test
    void testGetCustomerById_notFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomerById(customerId));
        assertTrue(ex.getMessage().contains(customerId.toString()));
    }

    @Test
    void testGetCustomerByDocument_found() {
        String documentId = "12345678";
        Customer customer = new Customer();
        customer.documentId = documentId;
        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.findByDocumentId(documentId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.getCustomerByDocument(documentId);

        assertSame(expectedResponse, result);
    }

    @Test
    void testGetCustomerByDocument_notFound() {
        String documentId = "NOTEXIST";
        when(customerRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomerByDocument(documentId));
        assertTrue(ex.getMessage().contains(documentId));
    }

    @Test
    void testUpdateCustomer_success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.email = "old@example.com";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.firstName = "Updated";
        request.email = "new@example.com";

        CustomerResponse expectedResponse = new CustomerResponse();
        expectedResponse.customerId = customerId;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.updateCustomer(customerId, request);

        assertEquals(customerId, result.customerId);
        verify(customerMapper).updateEntity(customer, request);
        verify(customerRepository).persist(customer);
        verify(successCounter).increment();
    }

    @Test
    void testUpdateCustomer_notFound() {
        UUID customerId = UUID.randomUUID();
        UpdateCustomerRequest request = new UpdateCustomerRequest();

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.updateCustomer(customerId, request));
    }

    @Test
    void testUpdateCustomer_emailConflict() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.email = "old@example.com";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.email = "taken@example.com";

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        DuplicateCustomerException ex = assertThrows(DuplicateCustomerException.class,
                () -> customerService.updateCustomer(customerId, request));
        assertTrue(ex.getMessage().contains("taken@example.com"));
        verify(failureCounter).increment();
        verify(customerRepository, never()).persist(any(Customer.class));
    }

    @Test
    void testUpdateCustomer_sameEmail_noConflictCheck() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.email = "same@example.com";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.email = "same@example.com"; // same email as existing

        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        customerService.updateCustomer(customerId, request);

        // existsByEmail should NOT be called since email is the same
        verify(customerRepository, never()).existsByEmail(anyString());
        verify(customerMapper).updateEntity(customer, request);
        verify(customerRepository).persist(customer);
        verify(successCounter).increment();
    }

    @Test
    void testUpdateCustomer_nullEmail_noConflictCheck() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.email = "existing@example.com";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        // request.email is null

        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        customerService.updateCustomer(customerId, request);

        // existsByEmail should NOT be called since email is null
        verify(customerRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testDeleteCustomer_success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.ACTIVE;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customerId);

        assertEquals(CustomerStatus.INACTIVE, customer.status);
        verify(customerRepository).persist(customer);
        verify(successCounter).increment();
    }

    @Test
    void testDeleteCustomer_notFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.deleteCustomer(customerId));
    }

    @Test
    void testUpdateStatus_success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.customerId = customerId;
        customer.status = CustomerStatus.PENDING;

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = CustomerStatus.ACTIVE;
        request.reason = "Verification complete";

        CustomerResponse expectedResponse = new CustomerResponse();
        expectedResponse.customerId = customerId;
        expectedResponse.status = CustomerStatus.ACTIVE;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.updateCustomerStatus(customerId, request);

        assertEquals(CustomerStatus.ACTIVE, customer.status);
        assertEquals(CustomerStatus.ACTIVE, result.status);
        verify(customerRepository).persist(customer);
        verify(successCounter).increment();
    }

    @Test
    void testUpdateStatus_notFound() {
        UUID customerId = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = CustomerStatus.ACTIVE;

        when(customerRepository.findByIdOptional(customerId)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.updateCustomerStatus(customerId, request));
    }
}

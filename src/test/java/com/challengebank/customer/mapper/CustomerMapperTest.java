package com.challengebank.customer.mapper;

import com.challengebank.customer.model.dto.request.CreateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateCustomerRequest;
import com.challengebank.customer.model.dto.response.CustomerPageResponse;
import com.challengebank.customer.model.dto.response.CustomerResponse;
import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.model.enums.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void testToEntity() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.firstName = "John";
        request.lastName = "Doe";
        request.documentType = DocumentType.DNI;
        request.documentId = "12345678";
        request.email = "john.doe@example.com";
        request.phone = "+51999999999";
        request.dateOfBirth = LocalDate.of(1990, 1, 15);
        request.address = "123 Main Street";

        Customer customer = mapper.toEntity(request);

        assertEquals("John", customer.firstName);
        assertEquals("Doe", customer.lastName);
        assertEquals(DocumentType.DNI, customer.documentType);
        assertEquals("12345678", customer.documentId);
        assertEquals("john.doe@example.com", customer.email);
        assertEquals("+51999999999", customer.phone);
        assertEquals(LocalDate.of(1990, 1, 15), customer.dateOfBirth);
        assertEquals("123 Main Street", customer.address);
        assertEquals(CustomerStatus.PENDING, customer.status);
    }

    @Test
    void testToResponse() {
        Customer customer = new Customer();
        customer.customerId = UUID.randomUUID();
        customer.firstName = "Jane";
        customer.lastName = "Smith";
        customer.documentType = DocumentType.PASSPORT;
        customer.documentId = "AB123456";
        customer.email = "jane.smith@example.com";
        customer.phone = "+1234567890";
        customer.dateOfBirth = LocalDate.of(1985, 6, 20);
        customer.status = CustomerStatus.ACTIVE;
        customer.createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        customer.updatedAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);

        CustomerResponse response = mapper.toResponse(customer);

        assertEquals(customer.customerId, response.customerId);
        assertEquals("Jane", response.firstName);
        assertEquals("Smith", response.lastName);
        assertEquals(DocumentType.PASSPORT, response.documentType);
        assertEquals("AB123456", response.documentId);
        assertEquals("jane.smith@example.com", response.email);
        assertEquals("+1234567890", response.phone);
        assertEquals(LocalDate.of(1985, 6, 20), response.dateOfBirth);
        assertEquals(CustomerStatus.ACTIVE, response.status);
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), response.createdAt);
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0, 0), response.updatedAt);
    }

    @Test
    void testUpdateEntity_allFields() {
        Customer customer = new Customer();
        customer.firstName = "Old";
        customer.lastName = "Name";
        customer.email = "old@example.com";
        customer.phone = "+1000000000";
        customer.address = "Old Address";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.firstName = "New";
        request.lastName = "Updated";
        request.email = "new@example.com";
        request.phone = "+2000000000";
        request.address = "New Address";

        mapper.updateEntity(customer, request);

        assertEquals("New", customer.firstName);
        assertEquals("Updated", customer.lastName);
        assertEquals("new@example.com", customer.email);
        assertEquals("+2000000000", customer.phone);
        assertEquals("New Address", customer.address);
    }

    @Test
    void testUpdateEntity_partialFields() {
        Customer customer = new Customer();
        customer.firstName = "Original";
        customer.lastName = "Last";
        customer.email = "original@example.com";
        customer.phone = "+1111111111";
        customer.address = "Original Address";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.firstName = "Changed";
        request.email = "changed@example.com";
        // lastName, phone, address are null

        mapper.updateEntity(customer, request);

        assertEquals("Changed", customer.firstName);
        assertEquals("Last", customer.lastName);
        assertEquals("changed@example.com", customer.email);
        assertEquals("+1111111111", customer.phone);
        assertEquals("Original Address", customer.address);
    }

    @Test
    void testUpdateEntity_noFields() {
        Customer customer = new Customer();
        customer.firstName = "Keep";
        customer.lastName = "Same";
        customer.email = "keep@example.com";
        customer.phone = "+3333333333";
        customer.address = "Keep Address";

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        // all fields are null

        mapper.updateEntity(customer, request);

        assertEquals("Keep", customer.firstName);
        assertEquals("Same", customer.lastName);
        assertEquals("keep@example.com", customer.email);
        assertEquals("+3333333333", customer.phone);
        assertEquals("Keep Address", customer.address);
    }

    @Test
    void testToPageResponse() {
        Customer c1 = new Customer();
        c1.customerId = UUID.randomUUID();
        c1.firstName = "Alice";
        c1.lastName = "A";
        c1.documentType = DocumentType.DNI;
        c1.documentId = "11111111";
        c1.email = "alice@example.com";
        c1.status = CustomerStatus.ACTIVE;

        Customer c2 = new Customer();
        c2.customerId = UUID.randomUUID();
        c2.firstName = "Bob";
        c2.lastName = "B";
        c2.documentType = DocumentType.CEDULA;
        c2.documentId = "22222222";
        c2.email = "bob@example.com";
        c2.status = CustomerStatus.PENDING;

        List<Customer> customers = List.of(c1, c2);

        CustomerPageResponse response = mapper.toPageResponse(customers, 0, 10, 25);

        assertEquals(2, response.content.size());
        assertEquals(0, response.page);
        assertEquals(10, response.size);
        assertEquals(25, response.totalElements);
        assertEquals(3, response.totalPages); // ceil(25/10) = 3

        assertEquals("Alice", response.content.get(0).firstName);
        assertEquals("Bob", response.content.get(1).firstName);
    }

    @Test
    void testToPageResponse_emptyList() {
        List<Customer> customers = Collections.emptyList();

        CustomerPageResponse response = mapper.toPageResponse(customers, 0, 10, 0);

        assertTrue(response.content.isEmpty());
        assertEquals(0, response.page);
        assertEquals(10, response.size);
        assertEquals(0, response.totalElements);
        assertEquals(0, response.totalPages); // ceil(0/10) = 0
    }

    @Test
    void testToPageResponse_sizeZero() {
        List<Customer> customers = Collections.emptyList();

        CustomerPageResponse response = mapper.toPageResponse(customers, 0, 0, 0);

        assertTrue(response.content.isEmpty());
        assertEquals(0, response.totalPages); // size == 0 branch
    }
}

package com.challengebank.customer.controller;

import com.challengebank.customer.exception.CustomerNotFoundException;
import com.challengebank.customer.exception.DuplicateCustomerException;
import com.challengebank.customer.model.dto.request.CreateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateStatusRequest;
import com.challengebank.customer.model.dto.response.CustomerPageResponse;
import com.challengebank.customer.model.dto.response.CustomerResponse;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.model.enums.DocumentType;
import com.challengebank.customer.service.CustomerService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class CustomerControllerTest {

    @InjectMock
    CustomerService customerService;

    // -------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------

    private static final UUID CUSTOMER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String DOCUMENT_ID = "DOC12345678";

    private CreateCustomerRequest buildCreateRequest() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.firstName = "John";
        request.lastName = "Doe";
        request.documentType = DocumentType.DNI;
        request.documentId = "DOC12345678";
        request.email = "john.doe@example.com";
        request.phone = "+1234567890";
        request.dateOfBirth = LocalDate.of(1990, 1, 15);
        request.address = "123 Main St, Springfield";
        return request;
    }

    private UpdateCustomerRequest buildUpdateRequest() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.firstName = "Jane";
        request.lastName = "Doe";
        request.email = "jane.doe@example.com";
        request.phone = "+9876543210";
        request.address = "456 Oak Ave, Shelbyville";
        return request;
    }

    private UpdateStatusRequest buildUpdateStatusRequest() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = CustomerStatus.SUSPENDED;
        request.reason = "Suspicious activity detected";
        return request;
    }

    private CustomerResponse buildCustomerResponse() {
        CustomerResponse response = new CustomerResponse();
        response.customerId = CUSTOMER_ID;
        response.firstName = "John";
        response.lastName = "Doe";
        response.documentType = DocumentType.DNI;
        response.documentId = DOCUMENT_ID;
        response.email = "john.doe@example.com";
        response.phone = "+1234567890";
        response.dateOfBirth = LocalDate.of(1990, 1, 15);
        response.status = CustomerStatus.ACTIVE;
        response.createdAt = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        response.updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        return response;
    }

    private CustomerPageResponse buildPageResponse() {
        CustomerPageResponse pageResponse = new CustomerPageResponse();
        pageResponse.content = List.of(buildCustomerResponse());
        pageResponse.page = 0;
        pageResponse.size = 20;
        pageResponse.totalElements = 1;
        pageResponse.totalPages = 1;
        return pageResponse;
    }

    private CustomerPageResponse buildEmptyPageResponse() {
        CustomerPageResponse pageResponse = new CustomerPageResponse();
        pageResponse.content = Collections.emptyList();
        pageResponse.page = 0;
        pageResponse.size = 20;
        pageResponse.totalElements = 0;
        pageResponse.totalPages = 0;
        return pageResponse;
    }

    // -------------------------------------------------------
    // GET /v1/customers
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getAllCustomers_withRoleUser_returnsOk() {
        when(customerService.getAllCustomers(anyInt(), anyInt(), any()))
                .thenReturn(buildPageResponse());

        given()
                .when()
                .get("/v1/customers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1))
                .body("content[0].customerId", equalTo(CUSTOMER_ID.toString()))
                .body("content[0].firstName", equalTo("John"))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(1))
                .body("totalPages", equalTo(1));

        verify(customerService).getAllCustomers(0, 20, null);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllCustomers_withRoleAdmin_returnsOk() {
        when(customerService.getAllCustomers(anyInt(), anyInt(), any()))
                .thenReturn(buildPageResponse());

        given()
                .when()
                .get("/v1/customers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1));

        verify(customerService).getAllCustomers(0, 20, null);
    }

    @Test
    void getAllCustomers_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/customers")
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllCustomers_withStatusFilter_returnsFiltered() {
        when(customerService.getAllCustomers(eq(0), eq(20), eq(CustomerStatus.ACTIVE)))
                .thenReturn(buildPageResponse());

        given()
                .queryParam("status", "ACTIVE")
                .when()
                .get("/v1/customers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1));

        verify(customerService).getAllCustomers(0, 20, CustomerStatus.ACTIVE);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllCustomers_withPagination_returnsPaged() {
        when(customerService.getAllCustomers(eq(2), eq(10), any()))
                .thenReturn(buildEmptyPageResponse());

        given()
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when()
                .get("/v1/customers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(0));

        verify(customerService).getAllCustomers(2, 10, null);
    }

    // -------------------------------------------------------
    // POST /v1/customers
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createCustomer_withRoleAdmin_returnsCreated() {
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenReturn(buildCustomerResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("documentType", equalTo("DNI"))
                .body("documentId", equalTo(DOCUMENT_ID))
                .body("email", equalTo("john.doe@example.com"))
                .body("status", equalTo("ACTIVE"));

        verify(customerService).createCustomer(any(CreateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void createCustomer_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(403);

        verifyNoInteractions(customerService);
    }

    @Test
    void createCustomer_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createCustomer_validationError_returnsBadRequest() {
        // Send an empty request body to trigger validation errors
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest();
        // firstName, lastName, documentType, documentId, email are all @NotBlank/@NotNull

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("errors", is(notNullValue()))
                .body("errors.size()", greaterThan(0));

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createCustomer_duplicateDocument_returnsConflict() {
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateCustomerException("Customer with document DNI:DOC12345678 already exists"));

        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("already exists"));

        verify(customerService).createCustomer(any(CreateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createCustomer_duplicateEmail_returnsConflict() {
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateCustomerException("Customer with email john.doe@example.com already exists"));

        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/customers")
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("already exists"));
    }

    // -------------------------------------------------------
    // GET /v1/customers/{customerId}
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getCustomerById_success_returnsOk() {
        when(customerService.getCustomerById(CUSTOMER_ID))
                .thenReturn(buildCustomerResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("email", equalTo("john.doe@example.com"))
                .body("status", equalTo("ACTIVE"));

        verify(customerService).getCustomerById(CUSTOMER_ID);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getCustomerById_withRoleAdmin_returnsOk() {
        when(customerService.getCustomerById(CUSTOMER_ID))
                .thenReturn(buildCustomerResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()));

        verify(customerService).getCustomerById(CUSTOMER_ID);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getCustomerById_notFound_returns404() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(customerService.getCustomerById(nonExistentId))
                .thenThrow(new CustomerNotFoundException("Customer not found: " + nonExistentId));

        given()
                .when()
                .get("/v1/customers/{customerId}", nonExistentId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Customer not found"));

        verify(customerService).getCustomerById(nonExistentId);
    }

    @Test
    void getCustomerById_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    // -------------------------------------------------------
    // PUT /v1/customers/{customerId}
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomer_withRoleAdmin_returnsOk() {
        CustomerResponse updatedResponse = buildCustomerResponse();
        updatedResponse.firstName = "Jane";
        updatedResponse.lastName = "Doe";
        updatedResponse.email = "jane.doe@example.com";

        when(customerService.updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class)))
                .thenReturn(updatedResponse);

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("firstName", equalTo("Jane"))
                .body("email", equalTo("jane.doe@example.com"));

        verify(customerService).updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void updateCustomer_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(403);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomer_notFound_returns404() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(customerService.updateCustomer(eq(nonExistentId), any(UpdateCustomerRequest.class)))
                .thenThrow(new CustomerNotFoundException("Customer not found: " + nonExistentId));

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/customers/{customerId}", nonExistentId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Customer not found"));

        verify(customerService).updateCustomer(eq(nonExistentId), any(UpdateCustomerRequest.class));
    }

    @Test
    void updateCustomer_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomer_duplicateEmail_returnsConflict() {
        when(customerService.updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class)))
                .thenThrow(new DuplicateCustomerException("Email already in use: jane.doe@example.com"));

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("already in use"));

        verify(customerService).updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class));
    }

    // -------------------------------------------------------
    // DELETE /v1/customers/{customerId}
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void deleteCustomer_withRoleAdmin_returnsNoContent() {
        doNothing().when(customerService).deleteCustomer(CUSTOMER_ID);

        given()
                .when()
                .delete("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(204);

        verify(customerService).deleteCustomer(CUSTOMER_ID);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void deleteCustomer_withRoleUser_returnsForbidden() {
        given()
                .when()
                .delete("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(403);

        verifyNoInteractions(customerService);
    }

    @Test
    void deleteCustomer_unauthorized_returns401() {
        given()
                .when()
                .delete("/v1/customers/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void deleteCustomer_notFound_returns404() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        doThrow(new CustomerNotFoundException("Customer not found: " + nonExistentId))
                .when(customerService).deleteCustomer(nonExistentId);

        given()
                .when()
                .delete("/v1/customers/{customerId}", nonExistentId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Customer not found"));

        verify(customerService).deleteCustomer(nonExistentId);
    }

    // -------------------------------------------------------
    // GET /v1/customers/document/{documentId}
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getCustomerByDocument_success_returnsOk() {
        when(customerService.getCustomerByDocument(DOCUMENT_ID))
                .thenReturn(buildCustomerResponse());

        given()
                .when()
                .get("/v1/customers/document/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("documentId", equalTo(DOCUMENT_ID))
                .body("firstName", equalTo("John"));

        verify(customerService).getCustomerByDocument(DOCUMENT_ID);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getCustomerByDocument_withRoleAdmin_returnsOk() {
        when(customerService.getCustomerByDocument(DOCUMENT_ID))
                .thenReturn(buildCustomerResponse());

        given()
                .when()
                .get("/v1/customers/document/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("documentId", equalTo(DOCUMENT_ID));

        verify(customerService).getCustomerByDocument(DOCUMENT_ID);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getCustomerByDocument_notFound_returns404() {
        String unknownDoc = "UNKNOWN999";
        when(customerService.getCustomerByDocument(unknownDoc))
                .thenThrow(new CustomerNotFoundException("Customer not found with document: " + unknownDoc));

        given()
                .when()
                .get("/v1/customers/document/{documentId}", unknownDoc)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Customer not found"));

        verify(customerService).getCustomerByDocument(unknownDoc);
    }

    @Test
    void getCustomerByDocument_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/customers/document/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    // -------------------------------------------------------
    // PATCH /v1/customers/{customerId}/status
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomerStatus_withRoleAdmin_returnsOk() {
        CustomerResponse suspendedResponse = buildCustomerResponse();
        suspendedResponse.status = CustomerStatus.SUSPENDED;

        when(customerService.updateCustomerStatus(eq(CUSTOMER_ID), any(UpdateStatusRequest.class)))
                .thenReturn(suspendedResponse);

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/customers/{customerId}/status", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("SUSPENDED"));

        verify(customerService).updateCustomerStatus(eq(CUSTOMER_ID), any(UpdateStatusRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void updateCustomerStatus_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/customers/{customerId}/status", CUSTOMER_ID)
                .then()
                .statusCode(403);

        verifyNoInteractions(customerService);
    }

    @Test
    void updateCustomerStatus_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/customers/{customerId}/status", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(customerService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomerStatus_notFound_returns404() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(customerService.updateCustomerStatus(eq(nonExistentId), any(UpdateStatusRequest.class)))
                .thenThrow(new CustomerNotFoundException("Customer not found: " + nonExistentId));

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/customers/{customerId}/status", nonExistentId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Customer not found"));

        verify(customerService).updateCustomerStatus(eq(nonExistentId), any(UpdateStatusRequest.class));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateCustomerStatus_validationError_returnsBadRequest() {
        // Send a request with null status (which is @NotNull)
        UpdateStatusRequest invalidRequest = new UpdateStatusRequest();
        invalidRequest.status = null;

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .patch("/v1/customers/{customerId}/status", CUSTOMER_ID)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"));

        verifyNoInteractions(customerService);
    }
}

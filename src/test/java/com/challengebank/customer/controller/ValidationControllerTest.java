package com.challengebank.customer.controller;

import com.challengebank.customer.model.dto.request.ValidateCustomerRequest;
import com.challengebank.customer.model.dto.response.ValidationResponse;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.service.ValidationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class ValidationControllerTest {

    @InjectMock
    ValidationService validationService;

    // -------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------

    private static final UUID CUSTOMER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String DOCUMENT_ID = "DOC12345678";

    private ValidateCustomerRequest buildValidateByCustomerIdRequest() {
        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.customerId = CUSTOMER_ID;
        request.documentId = null;
        return request;
    }

    private ValidateCustomerRequest buildValidateByDocumentIdRequest() {
        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.customerId = null;
        request.documentId = DOCUMENT_ID;
        return request;
    }

    private ValidateCustomerRequest buildInvalidValidateRequest() {
        ValidateCustomerRequest request = new ValidateCustomerRequest();
        request.customerId = null;
        request.documentId = null;
        return request;
    }

    private ValidationResponse buildValidResponse() {
        ValidationResponse response = new ValidationResponse();
        response.valid = true;
        response.customerId = CUSTOMER_ID;
        response.status = CustomerStatus.ACTIVE;
        response.message = "Customer is active and valid";
        return response;
    }

    private ValidationResponse buildInvalidResponse() {
        ValidationResponse response = new ValidationResponse();
        response.valid = false;
        response.customerId = CUSTOMER_ID;
        response.status = CustomerStatus.SUSPENDED;
        response.message = "Customer exists but is not active (status: SUSPENDED)";
        return response;
    }

    private ValidationResponse buildNotFoundResponse() {
        ValidationResponse response = new ValidationResponse();
        response.valid = false;
        response.customerId = null;
        response.status = null;
        response.message = "Customer not found";
        return response;
    }

    // -------------------------------------------------------
    // POST /v1/customers/validate
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomer_withRoleUser_byCustomerId_returnsOk() {
        when(validationService.validateCustomer(any(ValidateCustomerRequest.class)))
                .thenReturn(buildValidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateByCustomerIdRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("ACTIVE"))
                .body("message", equalTo("Customer is active and valid"));

        verify(validationService).validateCustomer(any(ValidateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void validateCustomer_withRoleAdmin_byDocumentId_returnsOk() {
        when(validationService.validateCustomer(any(ValidateCustomerRequest.class)))
                .thenReturn(buildValidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateByDocumentIdRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("ACTIVE"));

        verify(validationService).validateCustomer(any(ValidateCustomerRequest.class));
    }

    @Test
    void validateCustomer_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildValidateByCustomerIdRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(401);

        verifyNoInteractions(validationService);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomer_illegalArgument_returnsBadRequest() {
        when(validationService.validateCustomer(any(ValidateCustomerRequest.class)))
                .thenThrow(new IllegalArgumentException("At least one of customerId or documentId must be provided"));

        given()
                .contentType(ContentType.JSON)
                .body(buildInvalidValidateRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("At least one of customerId or documentId must be provided"));

        verify(validationService).validateCustomer(any(ValidateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomer_customerNotFound_returnsValidFalse() {
        when(validationService.validateCustomer(any(ValidateCustomerRequest.class)))
                .thenReturn(buildNotFoundResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateByCustomerIdRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("message", equalTo("Customer not found"))
                .body("customerId", nullValue());

        verify(validationService).validateCustomer(any(ValidateCustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomer_inactiveCustomer_returnsValidFalse() {
        when(validationService.validateCustomer(any(ValidateCustomerRequest.class)))
                .thenReturn(buildInvalidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateByCustomerIdRequest())
                .when()
                .post("/v1/customers/validate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("SUSPENDED"))
                .body("message", containsString("not active"));

        verify(validationService).validateCustomer(any(ValidateCustomerRequest.class));
    }

    // -------------------------------------------------------
    // GET /v1/customers/{customerId}/validate
    // -------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomerById_withRoleUser_returnsOk() {
        when(validationService.validateCustomerById(CUSTOMER_ID))
                .thenReturn(buildValidResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}/validate", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("ACTIVE"))
                .body("message", equalTo("Customer is active and valid"));

        verify(validationService).validateCustomerById(CUSTOMER_ID);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void validateCustomerById_withRoleAdmin_returnsOk() {
        when(validationService.validateCustomerById(CUSTOMER_ID))
                .thenReturn(buildValidResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}/validate", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true));

        verify(validationService).validateCustomerById(CUSTOMER_ID);
    }

    @Test
    void validateCustomerById_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/customers/{customerId}/validate", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(validationService);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomerById_customerNotFound_returnsValidFalse() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(validationService.validateCustomerById(nonExistentId))
                .thenReturn(buildNotFoundResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}/validate", nonExistentId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("message", equalTo("Customer not found"))
                .body("customerId", nullValue());

        verify(validationService).validateCustomerById(nonExistentId);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateCustomerById_inactiveCustomer_returnsValidFalse() {
        when(validationService.validateCustomerById(CUSTOMER_ID))
                .thenReturn(buildInvalidResponse());

        given()
                .when()
                .get("/v1/customers/{customerId}/validate", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("customerId", equalTo(CUSTOMER_ID.toString()))
                .body("status", equalTo("SUSPENDED"))
                .body("message", containsString("not active"));

        verify(validationService).validateCustomerById(CUSTOMER_ID);
    }
}

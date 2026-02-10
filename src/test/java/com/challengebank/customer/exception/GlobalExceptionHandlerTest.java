package com.challengebank.customer.exception;

import com.challengebank.customer.model.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    UriInfo uriInfo;

    @InjectMocks
    GlobalExceptionHandler handler;

    @Test
    void testHandleCustomerNotFound() {
        when(uriInfo.getPath()).thenReturn("/customers/123");
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer not found: 123");

        RestResponse<ErrorResponse> response = handler.handleCustomerNotFound(ex, uriInfo);

        assertEquals(404, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(404, body.status);
        assertEquals("Not Found", body.error);
        assertEquals("Customer not found: 123", body.message);
        assertEquals("/customers/123", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleDuplicateCustomer() {
        when(uriInfo.getPath()).thenReturn("/customers");
        DuplicateCustomerException ex = new DuplicateCustomerException("Email already exists");

        RestResponse<ErrorResponse> response = handler.handleDuplicateCustomer(ex, uriInfo);

        assertEquals(409, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(409, body.status);
        assertEquals("Conflict", body.error);
        assertEquals("Email already exists", body.message);
        assertEquals("/customers", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleInvalidStatusTransition() {
        when(uriInfo.getPath()).thenReturn("/customers/123/status");
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException("Cannot transition from INACTIVE to ACTIVE");

        RestResponse<ErrorResponse> response = handler.handleInvalidStatusTransition(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Cannot transition from INACTIVE to ACTIVE", body.message);
        assertEquals("/customers/123/status", body.path);
        assertNotNull(body.timestamp);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testHandleConstraintViolation() {
        when(uriInfo.getPath()).thenReturn("/customers");

        ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("createCustomer.request.firstName");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must not be blank");

        ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("email");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be a valid email");

        Set<ConstraintViolation<?>> violations = Set.of(violation1, violation2);
        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

        RestResponse<ErrorResponse> response = handler.handleConstraintViolation(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Validation failed", body.message);
        assertEquals("/customers", body.path);
        assertNotNull(body.errors);
        assertEquals(2, body.errors.size());

        // Check that the field extraction works (last segment after '.')
        boolean hasFirstName = body.errors.stream()
                .anyMatch(fe -> "firstName".equals(fe.field) && "must not be blank".equals(fe.message));
        boolean hasEmail = body.errors.stream()
                .anyMatch(fe -> "email".equals(fe.field) && "must be a valid email".equals(fe.message));
        assertTrue(hasFirstName, "Should contain firstName field error");
        assertTrue(hasEmail, "Should contain email field error");
    }

    @Test
    void testHandleIllegalArgument() {
        when(uriInfo.getPath()).thenReturn("/customers/validate");
        IllegalArgumentException ex = new IllegalArgumentException("At least one identifier required");

        RestResponse<ErrorResponse> response = handler.handleIllegalArgument(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("At least one identifier required", body.message);
        assertEquals("/customers/validate", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleGenericException() {
        when(uriInfo.getPath()).thenReturn("/customers");
        Exception ex = new RuntimeException("Something unexpected");

        RestResponse<ErrorResponse> response = handler.handleGenericException(ex, uriInfo);

        assertEquals(500, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(500, body.status);
        assertEquals("Internal Server Error", body.error);
        assertEquals("An unexpected error occurred", body.message);
        assertEquals("/customers", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testBuildError_nullUriInfo() {
        CustomerNotFoundException ex = new CustomerNotFoundException("Not found");

        RestResponse<ErrorResponse> response = handler.handleCustomerNotFound(ex, null);

        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals("", body.path);
    }
}

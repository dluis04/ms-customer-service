package com.challengebank.customer.controller;

import com.challengebank.customer.model.dto.request.ValidateCustomerRequest;
import com.challengebank.customer.model.dto.response.ValidationResponse;
import com.challengebank.customer.service.ValidationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationController {

    @Inject
    ValidationService validationService;

    @POST
    @Path("/validate")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response validateCustomer(@Valid ValidateCustomerRequest request) {
        ValidationResponse response = validationService.validateCustomer(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{customerId}/validate")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response validateCustomerById(@PathParam("customerId") UUID customerId) {
        ValidationResponse response = validationService.validateCustomerById(customerId);
        return Response.ok(response).build();
    }
}

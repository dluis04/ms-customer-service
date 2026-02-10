package com.challengebank.customer.controller;

import com.challengebank.customer.model.dto.request.CreateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateStatusRequest;
import com.challengebank.customer.model.dto.response.CustomerPageResponse;
import com.challengebank.customer.model.dto.response.CustomerResponse;
import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.service.CustomerService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {

    @Inject
    CustomerService customerService;

    @GET
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAllCustomers(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size,
            @QueryParam("status") CustomerStatus status) {
        CustomerPageResponse response = customerService.getAllCustomers(page, size, status);
        return Response.ok(response).build();
    }

    @POST
    @RolesAllowed("ROLE_ADMIN")
    public Response createCustomer(@Valid CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{customerId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getCustomerById(@PathParam("customerId") UUID customerId) {
        CustomerResponse response = customerService.getCustomerById(customerId);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{customerId}")
    @RolesAllowed("ROLE_ADMIN")
    public Response updateCustomer(@PathParam("customerId") UUID customerId,
                                   @Valid UpdateCustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{customerId}")
    @RolesAllowed("ROLE_ADMIN")
    public Response deleteCustomer(@PathParam("customerId") UUID customerId) {
        customerService.deleteCustomer(customerId);
        return Response.noContent().build();
    }

    @GET
    @Path("/document/{documentId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getCustomerByDocument(@PathParam("documentId") String documentId) {
        CustomerResponse response = customerService.getCustomerByDocument(documentId);
        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{customerId}/status")
    @RolesAllowed("ROLE_ADMIN")
    public Response updateCustomerStatus(@PathParam("customerId") UUID customerId,
                                         @Valid UpdateStatusRequest request) {
        CustomerResponse response = customerService.updateCustomerStatus(customerId, request);
        return Response.ok(response).build();
    }
}

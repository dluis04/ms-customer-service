package com.challengebank.customer.mapper;

import com.challengebank.customer.model.dto.request.CreateCustomerRequest;
import com.challengebank.customer.model.dto.request.UpdateCustomerRequest;
import com.challengebank.customer.model.dto.response.CustomerPageResponse;
import com.challengebank.customer.model.dto.response.CustomerResponse;
import com.challengebank.customer.model.entity.Customer;
import com.challengebank.customer.model.enums.CustomerStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CustomerMapper {

    public Customer toEntity(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.firstName = request.firstName;
        customer.lastName = request.lastName;
        customer.documentType = request.documentType;
        customer.documentId = request.documentId;
        customer.email = request.email;
        customer.phone = request.phone;
        customer.dateOfBirth = request.dateOfBirth;
        customer.address = request.address;
        customer.status = CustomerStatus.PENDING;
        return customer;
    }

    public void updateEntity(Customer customer, UpdateCustomerRequest request) {
        if (request.firstName != null) {
            customer.firstName = request.firstName;
        }
        if (request.lastName != null) {
            customer.lastName = request.lastName;
        }
        if (request.email != null) {
            customer.email = request.email;
        }
        if (request.phone != null) {
            customer.phone = request.phone;
        }
        if (request.address != null) {
            customer.address = request.address;
        }
    }

    public CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.customerId = customer.customerId;
        response.firstName = customer.firstName;
        response.lastName = customer.lastName;
        response.documentType = customer.documentType;
        response.documentId = customer.documentId;
        response.email = customer.email;
        response.phone = customer.phone;
        response.dateOfBirth = customer.dateOfBirth;
        response.status = customer.status;
        response.createdAt = customer.createdAt;
        response.updatedAt = customer.updatedAt;
        return response;
    }

    public CustomerPageResponse toPageResponse(List<Customer> customers, int page, int size, long totalElements) {
        CustomerPageResponse response = new CustomerPageResponse();
        response.content = customers.stream().map(this::toResponse).toList();
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return response;
    }
}

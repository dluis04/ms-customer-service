package com.challengebank.customer.model.dto.response;

import com.challengebank.customer.model.enums.CustomerStatus;

import java.util.UUID;

public class ValidationResponse {

    public boolean valid;
    public UUID customerId;
    public CustomerStatus status;
    public String message;
}

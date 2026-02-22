package com.challengebank.customer.model.dto.request;

import com.challengebank.customer.model.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateStatusRequest {

    @NotNull
    public CustomerStatus status;

    @Size(max = 500)
    public String reason;
}

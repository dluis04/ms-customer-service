package com.challengebank.customer.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {

    @Size(min = 1, max = 100)
    public String firstName;

    @Size(min = 1, max = 100)
    public String lastName;

    @Email
    public String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    public String phone;

    @Size(max = 500)
    public String address;
}

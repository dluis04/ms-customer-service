package com.challengebank.customer.model.dto.request;

import com.challengebank.customer.model.enums.DocumentType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CreateCustomerRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    public String firstName;

    @NotBlank
    @Size(min = 1, max = 100)
    public String lastName;

    @NotNull
    public DocumentType documentType;

    @NotBlank
    @Size(min = 5, max = 20)
    public String documentId;

    @NotBlank
    @Email
    public String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    public String phone;

    public LocalDate dateOfBirth;

    @Size(max = 500)
    public String address;
}

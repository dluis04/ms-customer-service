package com.challengebank.customer.model.dto.response;

import java.util.List;

public class CustomerPageResponse {

    public List<CustomerResponse> content;
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}

package com.retail.flow.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponseDto {
    private Long id;
    private String name;
    private String mobile;
    private String email;
    private Boolean active;
}
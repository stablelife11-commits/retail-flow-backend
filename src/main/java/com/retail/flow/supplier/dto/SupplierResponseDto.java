package com.retail.flow.supplier.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierResponseDto {
    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
}
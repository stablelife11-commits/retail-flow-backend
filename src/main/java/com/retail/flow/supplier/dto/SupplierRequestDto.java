package com.retail.flow.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierRequestDto {
    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;
    private String address;
}
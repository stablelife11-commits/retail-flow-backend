package com.retail.flow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id;
    private String email;
    private String role;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    private String businessName;

    @Size(max = 20, message = "GSTIN cannot exceed 20 characters")
    private String gstNumber;

    @Size(max = 20, message = "Business phone cannot exceed 20 characters")
    private String businessPhone;

    @Size(max = 500, message = "Business address cannot exceed 500 characters")
    private String businessAddress;
}
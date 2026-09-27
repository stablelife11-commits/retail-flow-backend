package com.retail.flow.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDto {
    private String token;
    private String email;
    private String name;
    private String role;
}
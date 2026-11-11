package com.retail.flow.appupdate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUpdateDto {

    @NotBlank(message = "appType is required (BUYER or SELLER)")
    private String appType;

    @NotNull(message = "latestVersionCode is required")
    @Min(value = 1, message = "latestVersionCode must be at least 1")
    private Integer latestVersionCode;

    @NotBlank(message = "versionName is required")
    private String versionName;

    @NotBlank(message = "apkUrl is required")
    private String apkUrl;

    private String releaseNotes;

    @Builder.Default
    private boolean forceUpdate = false;
}
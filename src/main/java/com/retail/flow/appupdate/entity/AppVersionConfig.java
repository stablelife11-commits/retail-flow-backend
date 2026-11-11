package com.retail.flow.appupdate.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_version_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionConfig {

    @Id
    @Column(name = "app_type", length = 20, nullable = false)
    private String appType; // "BUYER" or "SELLER"

    @Column(name = "latest_version_code", nullable = false)
    private Integer latestVersionCode;

    @Column(name = "version_name", length = 30, nullable = false)
    private String versionName;

    @Column(name = "apk_url", length = 1000, nullable = false)
    private String apkUrl;

    @Column(name = "release_notes", length = 1000)
    private String releaseNotes;

    @Column(name = "force_update", nullable = false)
    @Builder.Default
    private Boolean forceUpdate = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touchTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
package com.risecode.riseflow.tenant.domain;

import com.risecode.riseflow.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenants", schema = "public")
public class Tenant extends BaseEntity {
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$", message = "domain must be a valid lowercase slug")
    @Column(nullable = false, unique = true, length = 63)
    private String domain;

    @Email
    @NotBlank
    @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String ownerEmail;

    @Column(nullable = false)
    private boolean active = true;
}

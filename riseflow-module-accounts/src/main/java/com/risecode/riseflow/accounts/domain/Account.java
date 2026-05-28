package com.risecode.riseflow.accounts.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class Account extends TenantAwareEntity {

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @Size(max = 160)
    @Column(length = 160)
    private String industry;

    @Email
    @Size(max = 180)
    @Column(length = 180)
    private String email;

    @Size(max = 40)
    @Column(length = 40)
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> customFields = new HashMap<>();
}

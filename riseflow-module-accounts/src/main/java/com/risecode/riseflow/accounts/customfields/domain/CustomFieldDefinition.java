package com.risecode.riseflow.accounts.customfields.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "custom_field_definitions")
public class CustomFieldDefinition extends TenantAwareEntity {

    @NotBlank
    @Size(max = 60)
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @NotBlank
    @Size(max = 120)
    @Column(name = "field_name", nullable = false, length = 120)
    private String fieldName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FieldType fieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "picklist_options", columnDefinition = "jsonb")
    private List<String> picklistOptions;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}

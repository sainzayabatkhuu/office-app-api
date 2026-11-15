package com.sol.office.identity.domain;

import com.sol.office.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A system parameter (CORPRM): a code/name pair grouped by type, e.g. type {@code password_expiry_days}.
 */
@Getter
@Setter
@Entity
@Table(name = "core_parameter")
public class CoreParameter extends BaseEntity {

    @Column(name = "param_type", nullable = false, length = 50)
    private String paramType;

    @Column(name = "param_code", nullable = false, length = 50)
    private String paramCode;

    @Column(name = "param_name", nullable = false, length = 200)
    private String paramName;
}

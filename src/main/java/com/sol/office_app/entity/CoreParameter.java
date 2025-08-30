package com.sol.office_app.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.Where;
import org.hibernate.type.YesNoConverter;

@Entity
@SoftDelete
public class CoreParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paramName;

    private String paramCode;

    private String paramType;

    @SoftDelete(strategy = SoftDeleteType.ACTIVE, converter = YesNoConverter.class)
    @Column(name = "del_flg", nullable = false, length = 1)
    private String delFlg = "N";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private CoreParameter parent;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getParamName() { return paramName; }
    public void setParamName(String paramName) { this.paramName = paramName; }

    public String getParamCode() { return paramCode; }
    public void setParamCode(String paramCode) { this.paramCode = paramCode; }

    public CoreParameter getParent() { return parent; }
    public void setParent(CoreParameter parent) { this.parent = parent; }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public String getDelFlg() {
        return delFlg;
    }

    public void setDelFlg(String delFlg) {
        this.delFlg = delFlg;
    }
}
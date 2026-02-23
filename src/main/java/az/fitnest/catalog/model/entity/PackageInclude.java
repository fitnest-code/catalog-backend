/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Embeddable
 */
package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PackageInclude {
    @Column(name="type")
    private String type;
    @Column(name="name")
    private String name;
    @Column(name="code")
    private String code;
    @Column(name="is_included")
    private Boolean included = true;

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public Boolean getIncluded() {
        return this.included;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
    }

    public PackageInclude() {
    }

    public PackageInclude(String type, String name, String code, Boolean included) {
        this.type = type;
        this.name = name;
        this.code = code;
        this.included = included;
    }
}


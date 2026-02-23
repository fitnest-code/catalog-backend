/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="store_discounts")
public class StoreDiscount
extends BaseEntity {
    @Column(name="percent")
    private Integer percent;
    @Column(name="applies_to", columnDefinition="TEXT")
    private String appliesTo;

    public Integer getPercent() {
        return this.percent;
    }

    public String getAppliesTo() {
        return this.appliesTo;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public StoreDiscount() {
    }

    public StoreDiscount(Integer percent, String appliesTo) {
        this.percent = percent;
        this.appliesTo = appliesTo;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.CollectionTable
 *  jakarta.persistence.Column
 *  jakarta.persistence.ElementCollection
 *  jakarta.persistence.Entity
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.PackageInclude;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="gym_packages")
public class GymPackage
extends BaseAuditableEntity {
    @Column(name="name", nullable=false)
    private String name;
    @Column(name="entry_limit")
    private Integer entryLimit;
    @Column(name="single_entry_price")
    private Double singleEntryPrice;
    @Column(name="monthly_price")
    private Double monthlyPrice;
    @Column(name="currency")
    private String currency = "AZN";
    @ElementCollection
    @CollectionTable(name="package_includes", joinColumns={@JoinColumn(name="package_id")})
    private List<PackageInclude> includes = new ArrayList<PackageInclude>();

    public String getName() {
        return this.name;
    }

    public Integer getEntryLimit() {
        return this.entryLimit;
    }

    public Double getSingleEntryPrice() {
        return this.singleEntryPrice;
    }

    public Double getMonthlyPrice() {
        return this.monthlyPrice;
    }

    public String getCurrency() {
        return this.currency;
    }

    public List<PackageInclude> getIncludes() {
        return this.includes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEntryLimit(Integer entryLimit) {
        this.entryLimit = entryLimit;
    }

    public void setSingleEntryPrice(Double singleEntryPrice) {
        this.singleEntryPrice = singleEntryPrice;
    }

    public void setMonthlyPrice(Double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setIncludes(List<PackageInclude> includes) {
        this.includes = includes;
    }

    public GymPackage() {
    }

    public GymPackage(String name, Integer entryLimit, Double singleEntryPrice, Double monthlyPrice, String currency, List<PackageInclude> includes) {
        this.name = name;
        this.entryLimit = entryLimit;
        this.singleEntryPrice = singleEntryPrice;
        this.monthlyPrice = monthlyPrice;
        this.currency = currency;
        this.includes = includes;
    }
}


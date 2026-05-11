package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_discounts")
public class StoreDiscount extends BaseEntity {

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "percent")
    private Integer percent;

    @Column(name = "applies_to", columnDefinition = "TEXT")
    private String appliesTo;

    public StoreDiscount() {
    }

    public StoreDiscount(Long packageId, Integer percent) {
        this.packageId = packageId;
        this.percent = percent;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String a) {
        this.appliesTo = a;
    }
}

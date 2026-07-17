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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreDiscount)) return false;
        StoreDiscount that = (StoreDiscount) o;
        if (this.getId() != null && that.getId() != null) {
            return java.util.Objects.equals(this.getId(), that.getId());
        }
        return java.util.Objects.equals(packageId, that.packageId) &&
               java.util.Objects.equals(percent, that.percent) &&
               java.util.Objects.equals(appliesTo, that.appliesTo);
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return java.util.Objects.hashCode(this.getId());
        }
        return java.util.Objects.hash(packageId, percent, appliesTo);
    }
}

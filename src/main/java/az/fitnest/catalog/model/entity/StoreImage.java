package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_images")
public class StoreImage
        extends BaseEntity {
    @Column(name = "type")
    private String type;
    @Column(name = "title")
    private String title;
    @Column(name = "url", nullable = false)
    private String url;

    public StoreImage() {
    }

    public StoreImage(String type, String title, String url) {
        this.type = type;
        this.title = title;
        this.url = url;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreImage)) return false;
        StoreImage that = (StoreImage) o;
        if (this.getId() != null && that.getId() != null) {
            return java.util.Objects.equals(this.getId(), that.getId());
        }
        return java.util.Objects.equals(type, that.type) &&
               java.util.Objects.equals(title, that.title) &&
               java.util.Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return java.util.Objects.hashCode(this.getId());
        }
        return java.util.Objects.hash(type, title, url);
    }
}

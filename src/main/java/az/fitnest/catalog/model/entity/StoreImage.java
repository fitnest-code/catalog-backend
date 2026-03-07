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
}

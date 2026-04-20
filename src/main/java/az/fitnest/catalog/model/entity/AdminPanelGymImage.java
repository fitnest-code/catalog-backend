package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_panel_gym_images")
public class AdminPanelGymImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private GymAdminPanel gym;

    @Column(name = "image_name", nullable = false)
    private String imageName;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public AdminPanelGymImage() {
    }

    public AdminPanelGymImage(Long id, GymAdminPanel gym, String imageName, String url, String type, String title, Integer sortOrder) {
        this.id = id;
        this.gym = gym;
        this.imageName = imageName;
        this.url = url;
        this.type = type;
        this.title = title;
        this.sortOrder = sortOrder;
    }

    public static GymImageBuilder builder() {
        return new GymImageBuilder();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminPanelGymImage)) return false;
        AdminPanelGymImage other = (AdminPanelGymImage) o;
        if (!other.canEqual(this)) return false;
        Long thisId = this.getId();
        Long otherId = other.getId();
        return thisId == null ? otherId == null : thisId.equals(otherId);
    }

    protected boolean canEqual(Object other) {
        return other instanceof AdminPanelGymImage;
    }

    public int hashCode() {
        Long $id = this.getId();
        return 59 + ($id == null ? 43 : $id.hashCode());
    }

    public String toString() {
        return "AdminPanelGymImage(id=" + this.id + ", imageName=" + this.imageName + ", url=" + this.url + ", type=" + this.type + ", title=" + this.title + ", sortOrder=" + this.sortOrder + ")";
    }

    public static class GymImageBuilder {
        private Long id;
        private GymAdminPanel gym;
        private String imageName;
        private String url;
        private String type;
        private String title;
        private Integer sortOrder;

        GymImageBuilder() {
        }

        public GymImageBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GymImageBuilder gym(GymAdminPanel gym) {
            this.gym = gym;
            return this;
        }

        public GymImageBuilder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public GymImageBuilder url(String url) {
            this.url = url;
            return this;
        }

        public GymImageBuilder type(String type) {
            this.type = type;
            return this;
        }

        public GymImageBuilder title(String title) {
            this.title = title;
            return this;
        }

        public GymImageBuilder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public AdminPanelGymImage build() {
            return new AdminPanelGymImage(this.id, this.gym, this.imageName, this.url, this.type, this.title, this.sortOrder);
        }

        public String toString() {
            return "AdminPanelGymImage.GymImageBuilder(id=" + this.id + ", imageName=" + this.imageName + ", url=" + this.url + ", type=" + this.type + ", title=" + this.title + ", sortOrder=" + this.sortOrder + ")";
        }
    }
}
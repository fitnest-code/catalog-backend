/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.Gym;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gym_images")
public class GymImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;
    @Column(name = "image_name", nullable = false)
    private String imageName;
    @Column(nullable = false)
    private String url;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String title;

    public GymImage() {
    }

    public GymImage(Long id, Gym gym, String imageName, String url, String type, String title) {
        this.id = id;
        this.gym = gym;
        this.imageName = imageName;
        this.url = url;
        this.type = type;
        this.title = title;
    }

    public static GymImageBuilder builder() {
        return new GymImageBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Gym getGym() {
        return this.gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public String getImageName() {
        return this.imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymImage)) {
            return false;
        }
        GymImage other = (GymImage) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object) this$id).equals(other$id)) {
            return false;
        }
        Gym this$gym = this.getGym();
        Gym other$gym = other.getGym();
        if (this$gym == null ? other$gym != null : !this$gym.equals(other$gym)) {
            return false;
        }
        String this$imageName = this.getImageName();
        String other$imageName = other.getImageName();
        if (this$imageName == null ? other$imageName != null : !this$imageName.equals(other$imageName)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        return !(this$title == null ? other$title != null : !this$title.equals(other$title));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymImage;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object) $id).hashCode());
        Gym $gym = this.getGym();
        result = result * 59 + ($gym == null ? 43 : $gym.hashCode());
        String $imageName = this.getImageName();
        result = result * 59 + ($imageName == null ? 43 : $imageName.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        return result;
    }

    public String toString() {
        return "GymImage(id=" + this.getId() + ", gym=" + this.getGym() + ", imageName=" + this.getImageName() + ", url=" + this.getUrl() + ", type=" + this.getType() + ", title=" + this.getTitle() + ")";
    }

    public static class GymImageBuilder {
        private Long id;
        private Gym gym;
        private String imageName;
        private String url;
        private String type;
        private String title;

        GymImageBuilder() {
        }

        public GymImageBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GymImageBuilder gym(Gym gym) {
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

        public GymImage build() {
            return new GymImage(this.id, this.gym, this.imageName, this.url, this.type, this.title);
        }

        public String toString() {
            return "GymImage.GymImageBuilder(id=" + this.id + ", gym=" + this.gym + ", imageName=" + this.imageName + ", url=" + this.url + ", type=" + this.type + ", title=" + this.title + ")";
        }
    }
}


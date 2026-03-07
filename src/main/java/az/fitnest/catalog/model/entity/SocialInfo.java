package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SocialInfo {
    @Column(name = "instagram_url")
    private String instagramUrl;
    @Column(name = "facebook_url")
    private String facebookUrl;
    @Column(name = "website_url")
    private String websiteUrl;

    public SocialInfo() {
    }

    public SocialInfo(String instagramUrl, String facebookUrl, String websiteUrl) {
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.websiteUrl = websiteUrl;
    }

    public String getInstagramUrl() {
        return this.instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getFacebookUrl() {
        return this.facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getWebsiteUrl() {
        return this.websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }
}

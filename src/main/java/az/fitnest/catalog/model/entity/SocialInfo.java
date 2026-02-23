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
public class SocialInfo {
    @Column(name="instagram_url")
    private String instagramUrl;
    @Column(name="facebook_url")
    private String facebookUrl;
    @Column(name="website_url")
    private String websiteUrl;

    public String getInstagramUrl() {
        return this.instagramUrl;
    }

    public String getFacebookUrl() {
        return this.facebookUrl;
    }

    public String getWebsiteUrl() {
        return this.websiteUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public SocialInfo() {
    }

    public SocialInfo(String instagramUrl, String facebookUrl, String websiteUrl) {
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.websiteUrl = websiteUrl;
    }
}


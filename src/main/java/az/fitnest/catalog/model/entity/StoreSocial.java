/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.AttributeOverride
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="store_socials")
@AttributeOverride(name="id", column=@Column(name="store_id"))
public class StoreSocial
extends BaseEntity {
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

    public StoreSocial() {
    }

    public StoreSocial(String instagramUrl, String facebookUrl, String websiteUrl) {
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.websiteUrl = websiteUrl;
    }
}


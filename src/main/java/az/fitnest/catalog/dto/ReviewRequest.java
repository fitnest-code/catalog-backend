/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.Max
 *  jakarta.validation.constraints.Min
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.Size
 */
package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequest {
    @NotNull
    @Min(value=1L)
    @Max(value=5L)
    private @NotNull @Min(value=1L) @Max(value=5L) Integer rating;
    @Size(max=1000)
    private @Size(max=1000) String comment;

    public static ReviewRequestBuilder builder() {
        return new ReviewRequestBuilder();
    }

    public Integer getRating() {
        return this.rating;
    }

    public String getComment() {
        return this.comment;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReviewRequest)) {
            return false;
        }
        ReviewRequest other = (ReviewRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$rating = this.getRating();
        Integer other$rating = other.getRating();
        if (this$rating == null ? other$rating != null : !((Object)this$rating).equals(other$rating)) {
            return false;
        }
        String this$comment = this.getComment();
        String other$comment = other.getComment();
        return !(this$comment == null ? other$comment != null : !this$comment.equals(other$comment));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ReviewRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $rating = this.getRating();
        result = result * 59 + ($rating == null ? 43 : ((Object)$rating).hashCode());
        String $comment = this.getComment();
        result = result * 59 + ($comment == null ? 43 : $comment.hashCode());
        return result;
    }

    public String toString() {
        return "ReviewRequest(rating=" + this.getRating() + ", comment=" + this.getComment() + ")";
    }

    public ReviewRequest() {
    }

    public ReviewRequest(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public static class ReviewRequestBuilder {
        private Integer rating;
        private String comment;

        ReviewRequestBuilder() {
        }

        public ReviewRequestBuilder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public ReviewRequestBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public ReviewRequest build() {
            return new ReviewRequest(this.rating, this.comment);
        }

        public String toString() {
            return "ReviewRequest.ReviewRequestBuilder(rating=" + this.rating + ", comment=" + this.comment + ")";
        }
    }
}


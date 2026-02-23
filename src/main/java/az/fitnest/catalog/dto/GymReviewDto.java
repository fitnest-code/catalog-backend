/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymReviewAuthorDto;

public class GymReviewDto {
    private String review_id;
    private Integer rating;
    private String comment;
    private GymReviewAuthorDto author;
    private String created_at;

    public static GymReviewDtoBuilder builder() {
        return new GymReviewDtoBuilder();
    }

    public String getReview_id() {
        return this.review_id;
    }

    public Integer getRating() {
        return this.rating;
    }

    public String getComment() {
        return this.comment;
    }

    public GymReviewAuthorDto getAuthor() {
        return this.author;
    }

    public String getCreated_at() {
        return this.created_at;
    }

    public void setReview_id(String review_id) {
        this.review_id = review_id;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setAuthor(GymReviewAuthorDto author) {
        this.author = author;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymReviewDto)) {
            return false;
        }
        GymReviewDto other = (GymReviewDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$rating = this.getRating();
        Integer other$rating = other.getRating();
        if (this$rating == null ? other$rating != null : !((Object)this$rating).equals(other$rating)) {
            return false;
        }
        String this$review_id = this.getReview_id();
        String other$review_id = other.getReview_id();
        if (this$review_id == null ? other$review_id != null : !this$review_id.equals(other$review_id)) {
            return false;
        }
        String this$comment = this.getComment();
        String other$comment = other.getComment();
        if (this$comment == null ? other$comment != null : !this$comment.equals(other$comment)) {
            return false;
        }
        GymReviewAuthorDto this$author = this.getAuthor();
        GymReviewAuthorDto other$author = other.getAuthor();
        if (this$author == null ? other$author != null : !((Object)this$author).equals(other$author)) {
            return false;
        }
        String this$created_at = this.getCreated_at();
        String other$created_at = other.getCreated_at();
        return !(this$created_at == null ? other$created_at != null : !this$created_at.equals(other$created_at));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymReviewDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $rating = this.getRating();
        result = result * 59 + ($rating == null ? 43 : ((Object)$rating).hashCode());
        String $review_id = this.getReview_id();
        result = result * 59 + ($review_id == null ? 43 : $review_id.hashCode());
        String $comment = this.getComment();
        result = result * 59 + ($comment == null ? 43 : $comment.hashCode());
        GymReviewAuthorDto $author = this.getAuthor();
        result = result * 59 + ($author == null ? 43 : ((Object)$author).hashCode());
        String $created_at = this.getCreated_at();
        result = result * 59 + ($created_at == null ? 43 : $created_at.hashCode());
        return result;
    }

    public String toString() {
        return "GymReviewDto(review_id=" + this.getReview_id() + ", rating=" + this.getRating() + ", comment=" + this.getComment() + ", author=" + this.getAuthor() + ", created_at=" + this.getCreated_at() + ")";
    }

    public GymReviewDto() {
    }

    public GymReviewDto(String review_id, Integer rating, String comment, GymReviewAuthorDto author, String created_at) {
        this.review_id = review_id;
        this.rating = rating;
        this.comment = comment;
        this.author = author;
        this.created_at = created_at;
    }

    public static class GymReviewDtoBuilder {
        private String review_id;
        private Integer rating;
        private String comment;
        private GymReviewAuthorDto author;
        private String created_at;

        GymReviewDtoBuilder() {
        }

        public GymReviewDtoBuilder review_id(String review_id) {
            this.review_id = review_id;
            return this;
        }

        public GymReviewDtoBuilder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public GymReviewDtoBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public GymReviewDtoBuilder author(GymReviewAuthorDto author) {
            this.author = author;
            return this;
        }

        public GymReviewDtoBuilder created_at(String created_at) {
            this.created_at = created_at;
            return this;
        }

        public GymReviewDto build() {
            return new GymReviewDto(this.review_id, this.rating, this.comment, this.author, this.created_at);
        }

        public String toString() {
            return "GymReviewDto.GymReviewDtoBuilder(review_id=" + this.review_id + ", rating=" + this.rating + ", comment=" + this.comment + ", author=" + this.author + ", created_at=" + this.created_at + ")";
        }
    }
}


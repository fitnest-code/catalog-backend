/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequest {
    @NotBlank
    private String name;

    public CategoryRequest() {
    }

    public CategoryRequest(String name) {
        this.name = name;
    }

    public static CategoryRequestBuilder builder() {
        return new CategoryRequestBuilder();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CategoryRequest)) {
            return false;
        }
        CategoryRequest other = (CategoryRequest) o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        return !(this$name == null ? other$name != null : !this$name.equals(other$name));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CategoryRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    public String toString() {
        return "CategoryRequest(name=" + this.getName() + ")";
    }

    public static class CategoryRequestBuilder {
        private String name;

        CategoryRequestBuilder() {
        }

        public CategoryRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CategoryRequest build() {
            return new CategoryRequest(this.name);
        }

        public String toString() {
            return "CategoryRequest.CategoryRequestBuilder(name=" + this.name + ")";
        }
    }
}


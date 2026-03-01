package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.entity.StoreSocialLink;

import java.util.List;

public class StoreSocialDto {
    private List<StoreSocialLink> links;

    public StoreSocialDto() {
    }

    public StoreSocialDto(List<StoreSocialLink> links) {
        this.links = links;
    }

    public static StoreSocialDtoBuilder builder() {
        return new StoreSocialDtoBuilder();
    }

    public List<StoreSocialLink> getLinks() {
        return this.links;
    }

    public void setLinks(List<StoreSocialLink> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreSocialDto)) return false;
        StoreSocialDto that = (StoreSocialDto) o;
        return java.util.Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(links);
    }

    @Override
    public String toString() {
        return "StoreSocialDto(links=" + links + ")";
    }

    public static class StoreSocialDtoBuilder {
        private List<StoreSocialLink> links;

        StoreSocialDtoBuilder() {
        }

        public StoreSocialDtoBuilder links(List<StoreSocialLink> links) {
            this.links = links;
            return this;
        }

        public StoreSocialDto build() {
            return new StoreSocialDto(this.links);
        }

        public String toString() {
            return "StoreSocialDto.StoreSocialDtoBuilder(links=" + this.links + ")";
        }
    }
}

package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

public class UpdateImageUrlRequest {
    private String url;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof UpdateImageUrlRequest)) {
            return false;
        }
        UpdateImageUrlRequest other = (UpdateImageUrlRequest) o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        return !(this$url == null ? other$url != null : !this$url.equals(other$url));
    }

    protected boolean canEqual(Object other) {
        return other instanceof UpdateImageUrlRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        return result;
    }

    public String toString() {
        return "UpdateImageUrlRequest(url=" + this.getUrl() + ")";
    }
}

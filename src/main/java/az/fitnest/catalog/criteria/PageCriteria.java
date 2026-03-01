/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 */
package az.fitnest.catalog.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;

public class PageCriteria {
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    private Integer page = 0;
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    private Integer count = 10;

    public PageCriteria() {
    }

    public PageCriteria(Integer page, Integer count) {
        this.page = page;
        this.count = count;
    }

    public Integer getPage() {
        return this.page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PageCriteria)) {
            return false;
        }
        PageCriteria other = (PageCriteria) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$page = this.getPage();
        Integer other$page = other.getPage();
        if (this$page == null ? other$page != null : !((Object) this$page).equals(other$page)) {
            return false;
        }
        Integer this$count = this.getCount();
        Integer other$count = other.getCount();
        return !(this$count == null ? other$count != null : !((Object) this$count).equals(other$count));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PageCriteria;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $page = this.getPage();
        result = result * 59 + ($page == null ? 43 : ((Object) $page).hashCode());
        Integer $count = this.getCount();
        result = result * 59 + ($count == null ? 43 : ((Object) $count).hashCode());
        return result;
    }

    public String toString() {
        return "PageCriteria(page=" + this.getPage() + ", count=" + this.getCount() + ")";
    }
}


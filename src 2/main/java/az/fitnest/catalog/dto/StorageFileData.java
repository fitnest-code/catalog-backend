/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class StorageFileData {
    private String path;
    private long size;
    private String md5;
    private long fsId;

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5() {
        return this.md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getFsId() {
        return this.fsId;
    }

    public void setFsId(long fsId) {
        this.fsId = fsId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StorageFileData)) {
            return false;
        }
        StorageFileData other = (StorageFileData) o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getSize() != other.getSize()) {
            return false;
        }
        if (this.getFsId() != other.getFsId()) {
            return false;
        }
        String this$path = this.getPath();
        String other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) {
            return false;
        }
        String this$md5 = this.getMd5();
        String other$md5 = other.getMd5();
        return !(this$md5 == null ? other$md5 != null : !this$md5.equals(other$md5));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StorageFileData;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $size = this.getSize();
        result = result * 59 + (int) ($size >>> 32 ^ $size);
        long $fsId = this.getFsId();
        result = result * 59 + (int) ($fsId >>> 32 ^ $fsId);
        String $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        String $md5 = this.getMd5();
        result = result * 59 + ($md5 == null ? 43 : $md5.hashCode());
        return result;
    }

    public String toString() {
        return "StorageFileData(path=" + this.getPath() + ", size=" + this.getSize() + ", md5=" + this.getMd5() + ", fsId=" + this.getFsId() + ")";
    }
}


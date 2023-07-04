package org.edu_sharing.elasticsearch.alfresco.client;

public class NodePreview {
    private String mimetype;
    private byte[] small;
    private byte[] large;
    private boolean isIcon;
    private String type;

    public byte[] getSmall() {
        return small;
    }

    public void setSmall(byte[] small) {
        this.small = small;
    }

    public byte[] getLarge() {
        return large;
    }

    public void setLarge(byte[] large) {
        this.large = large;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setIsIcon(boolean isIcon) {
        this.isIcon = isIcon;
    }

    public boolean isIcon() {
        return isIcon;
    }
}

package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class About {

    String themesUrl;
    long lastCacheUpdate;

    @JsonIgnore
    Object version;

    @JsonIgnore
    Object services;

    @JsonIgnore
    Object renderingService2;

    @JsonIgnore
    Object plugins;

    @JsonIgnore
    Object features;


    public String getThemesUrl() {
        return themesUrl;
    }

    public void setThemesUrl(String themesUrl) {
        this.themesUrl = themesUrl;
    }

    public long getLastCacheUpdate() {
        return lastCacheUpdate;
    }

    public void setLastCacheUpdate(long lastCacheUpdate) {
        this.lastCacheUpdate = lastCacheUpdate;
    }

    public Object getVersion() {
        return version;
    }

    public void setVersion(Object version) {
        this.version = version;
    }

    public Object getServices() {
        return services;
    }

    public void setServices(Object services) {
        this.services = services;
    }

    public Object getPlugins() {
        return plugins;
    }

    public void setPlugins(Object plugins) {
        this.plugins = plugins;
    }

    public Object getFeatures() {
        return features;
    }

    public void setFeatures(Object features) {
        this.features = features;
    }
}

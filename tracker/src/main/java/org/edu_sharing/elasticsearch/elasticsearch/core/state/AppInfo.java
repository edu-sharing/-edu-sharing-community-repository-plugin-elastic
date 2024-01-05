package org.edu_sharing.elasticsearch.elasticsearch.core.state;

import lombok.Data;

import java.util.Date;

@Data
public class AppInfo {
    private Date creationDate;
    private String trackerVersion;
}

package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.List;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WidgetV2 {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String value, type;
        private boolean negate;

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Subwidget {
        private String id;
    }

    private String id;
    private String  caption;
    private String  bottomCaption;
    private String  icon;
    private String  type;
    private String  template;
    private boolean hasValues;
    private List<ValueV2> values;
    private List<Subwidget> subwidgets;
    private String placeholder;
    private String unit;
    private Integer min;
    private Integer max;
    private Integer defaultMin;
    private Integer defaultMax;
    private Integer step;
    private boolean isExtended;
    private boolean allowempty;
    private String defaultvalue;
    private boolean isSearchable;
    private Condition condition;
    @JsonProperty
    public List<Subwidget> getSubwidgets() {
        return subwidgets;
    }
}


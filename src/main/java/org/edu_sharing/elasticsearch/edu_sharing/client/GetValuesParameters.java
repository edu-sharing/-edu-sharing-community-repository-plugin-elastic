package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.List;

public class GetValuesParameters {

    ValueParameters valueParameters;

    List<Criterias> criteria;

    public ValueParameters getValueParameters() {
        return valueParameters;
    }

    public void setValueParameters(ValueParameters valueParameters) {
        this.valueParameters = valueParameters;
    }

    public List<Criterias> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<Criterias> criteria) {
        this.criteria = criteria;
    }

    public static class ValueParameters {
        String query;
        String property;
        String pattern = "";

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }

    public static class Criterias{
        String property;
        List<String> values;

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }
    }
}

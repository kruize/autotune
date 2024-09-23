package com.autotune.analyzer.serviceObjects;

import java.util.List;
import java.util.Map;

public class CrawlerInput {
    private FilterWrapper filter;
    private TimeRange time_range;
    private String datasource;

    // Getters and Setters

    public TimeRange getTime_range() {
        return time_range;
    }

    public void setTime_range(TimeRange time_range) {
        this.time_range = time_range;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public FilterWrapper getFilter() {
        return filter;
    }

    public void setFilter(FilterWrapper filter) {
        this.filter = filter;
    }

    // Nested class for FilterWrapper that contains 'exclude' and 'include'
    public static class FilterWrapper {
        private Filter exclude;
        private Filter include;

        // Getters and Setters
        public Filter getExclude() {
            return exclude;
        }

        public void setExclude(Filter exclude) {
            this.exclude = exclude;
        }

        public Filter getInclude() {
            return include;
        }

        public void setInclude(Filter include) {
            this.include = include;
        }
    }

    public static class Filter {
        private List<String> namespace;
        private List<String> workload;
        private List<String> containers;
        private Map<String, String> labels;

        // Getters and Setters
        public List<String> getNamespace() {
            return namespace;
        }

        public void setNamespace(List<String> namespace) {
            this.namespace = namespace;
        }

        public List<String> getWorkload() {
            return workload;
        }

        public void setWorkload(List<String> workload) {
            this.workload = workload;
        }

        public List<String> getContainers() {
            return containers;
        }

        public void setContainers(List<String> containers) {
            this.containers = containers;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }
    }

    public static class TimeRange {
        private String start;
        private String end;

        // Getters and Setters
        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.serviceObjects;

import java.util.List;
import java.util.Map;

/**
 * Request payload object for Bulk Api service
 */
public class BulkInput {
    private FilterWrapper filter;
    private TimeRange time_range;
    private String datasource;
    private Webhook webhook;
    private String requestId; //TODO: to be used for the Kafka consumer case to map requestID with jobID

    // Getters and Setters

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public BulkInput() {
    }

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

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
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

    public static class Webhook {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

  
}

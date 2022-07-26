/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.target.kubernetes.model;

import com.google.gson.annotations.SerializedName;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data object used to store Container config details like CPU,Memory,Limits,ENV variables etc.
 */

public class ContainerConfigData {
    // Hashmap of Container request for resource quantity like cpu memory
    // Key will be requests
    /** Example
     *	"requests": {
     *	 	"cpu": {
     *		 	"amount": "2.11",
     *		 	"format": "",
     *		 	"additionalProperties": {}
     *		},
     *		"memory": {
     *		 	"amount": "160.0",
     *		 	"format": "",
     *		 	"additionalProperties": {}
     *		}
     * }
     */
    @SerializedName("requests")
    private Map<String, Quantity> requestPropertiesMap = new HashMap<String, Quantity>();
    // Hashmap of Container limit for resource quantity like cpu memory
    // Key will be limits
    /**
     * Example
     * "limits": {
     *		 "cpu": {
     *		 	"amount": "2.11",
     *		 	"format": "",
     *		 	"additionalProperties": {}
     *		 },
     *		 "memory": {
     *		 	"amount": "160.0",
     *		 	"format": "",
     *		 	"additionalProperties": {}
     *		 }
     * }
     */
    @SerializedName("limits")
    private Map<String, Quantity> LimitPropertiesMap = new HashMap<String, Quantity>();
    // List of Environment variables in key and value format
    /**
     * Example
     * "env": [
     *      {
     *          "name": "JAVA_OPTIONS",
     *          "additionalProperties": {},
     *          "value": " -server -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=21 -XX:+UseZGC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=27 -Dquarkus.thread-pool.core-threads=9"
     *      },
     *      {
     *          "name": "JDK_JAVA_OPTIONS",
     *          "additionalProperties": {},
     *          "value": " -server -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=21 -XX:+UseZGC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=27 -Dquarkus.thread-pool.core-threads=9"
     *      }
     * ]
     */
    @SerializedName("env")
    private List<EnvVar> envList = new ArrayList<EnvVar>();

    public ContainerConfigData() {
    }

    public Map<String, Quantity> getRequestPropertiesMap() {
        return requestPropertiesMap;
    }

    public void setRequestPropertiesMap(Map<String, Quantity> requestPropertiesMap) {
        this.requestPropertiesMap = requestPropertiesMap;
    }

    public Map<String, Quantity> getLimitPropertiesMap() {
        return LimitPropertiesMap;
    }

    public void setLimitPropertiesMap(Map<String, Quantity> limitPropertiesMap) {
        LimitPropertiesMap = limitPropertiesMap;
    }

    public List<EnvVar> getEnvList() {
        return envList;
    }

    public void setEnvList(List<EnvVar> envList) {
        this.envList = envList;
    }

    @Override
    public String toString() {
        return "ContainerConfigData{" +
                "requestPropertiesMap=" + requestPropertiesMap +
                ", LimitPropertiesMap=" + LimitPropertiesMap +
                ", envList=" + envList +
                '}';
    }
}

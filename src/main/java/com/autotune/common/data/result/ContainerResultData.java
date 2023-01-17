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
package com.autotune.common.data.result;

import java.util.HashMap;

/**
 * Experiments results storage object which is related to container metrics.
 */
public class ContainerResultData {
    private String image_name;
    private String container_name;
    private HashMap<String, HashMap<String, HashMap<String, GeneralInfoResult>>> container_metrics;

    public String getImage_name() {
        return image_name;
    }

    public void setImage_name(String image_name) {
        this.image_name = image_name;
    }

    public String getContainer_name() {
        return container_name;
    }

    public void setContainer_name(String container_name) {
        this.container_name = container_name;
    }

    public HashMap<String, HashMap<String, HashMap<String, GeneralInfoResult>>> getContainer_metrics() {
        return container_metrics;
    }

    public void setContainer_metrics(HashMap<String, HashMap<String, HashMap<String, GeneralInfoResult>>> container_metrics) {
        this.container_metrics = container_metrics;
    }

    @Override
    public String toString() {
        return "ContainerResultData{" +
                "image_name='" + image_name + '\'' +
                ", container_name='" + container_name + '\'' +
                ", container_metrics=" + container_metrics +
                '}';
    }
}

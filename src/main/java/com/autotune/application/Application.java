/*******************************************************************************
 * Copyright (c) 2020 Red Hat, IBM Corporation and others.
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
package com.autotune.application;

import com.autotune.collection.AutotuneConfig;

import java.util.ArrayList;

/**
 * An Application can have multiple layers on different levels.
 */
public class Application
{
    String applicationName;
    String namespace;
    String type;
    ArrayList<AutotuneConfig> autotuneConfigs;

    public Application(String applicationName, String namespace, String type)
    {
        this.applicationName = applicationName;
        this.namespace = namespace;
        this.type = type;

        autotuneConfigs = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public ArrayList<AutotuneConfig> getLayers() {
        return autotuneConfigs;
    }

    public void setLayers(ArrayList<AutotuneConfig> autotuneConfigs) {
        this.autotuneConfigs = autotuneConfigs;
    }

    @Override
    public String toString() {
        return "ApplicationTunables{" +
                "applicationName='" + applicationName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", type='" + type + '\'' +
                ", layers=" + autotuneConfigs +
                '}';
    }
}

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
package com.autotune.collection;

import com.autotune.application.Application;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class for the Autotune kubernetes kind objects.
 * Also contains details about applications matching the autotune object
 *
 * Refer to examples dir for a reference AutotuneObject yaml.
 */
public class AutotuneObject
{
    private String name;
    private String namespace;
    private String mode;
    private int replicas;
    private SlaInfo slaInfo;
    private SelectorInfo selectorInfo;


    /**
     * Map of applications matching the label selector in the autotune object yaml
     */
    public Map<String, Application> applicationsMap = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SlaInfo getSlaInfo() {
        return slaInfo;
    }

    public void setSlaInfo(SlaInfo slaInfo) {
        this.slaInfo = slaInfo;
    }

    public SelectorInfo getSelectorInfo() {
        return selectorInfo;
    }

    public void setSelectorInfo(SelectorInfo selectorInfo) {
        this.selectorInfo = selectorInfo;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return "AutotuneObject{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", mode='" + mode + '\'' +
                ", replicas=" + replicas +
                ", slaInfo=" + slaInfo +
                ", selectorInfo=" + selectorInfo +
                '}';
    }
}

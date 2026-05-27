/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

/**
 * API Object for enabling JMX agent on workloads
 */
public class EnableJMXAgentAPIObject {
    private String namespace;
    private String workloadName;
    private String workloadType;
    private String containerName;
    private Integer jmxPort = 9010;
    private String jmxHost = "0.0.0.0";
    private Boolean jmxAuthenticate = false;
    private Boolean jmxSsl = false;
    private Boolean jmxRegistrySsl = false;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public String getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(String workloadType) {
        this.workloadType = workloadType;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Integer getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(Integer jmxPort) {
        this.jmxPort = jmxPort;
    }

    public String getJmxHost() {
        return jmxHost;
    }

    public void setJmxHost(String jmxHost) {
        this.jmxHost = jmxHost;
    }

    public Boolean getJmxAuthenticate() {
        return jmxAuthenticate;
    }

    public void setJmxAuthenticate(Boolean jmxAuthenticate) {
        this.jmxAuthenticate = jmxAuthenticate;
    }

    public Boolean getJmxSsl() {
        return jmxSsl;
    }

    public void setJmxSsl(Boolean jmxSsl) {
        this.jmxSsl = jmxSsl;
    }

    public Boolean getJmxRegistrySsl() {
        return jmxRegistrySsl;
    }

    public void setJmxRegistrySsl(Boolean jmxRegistrySsl) {
        this.jmxRegistrySsl = jmxRegistrySsl;
    }
}


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
import com.autotune.exceptions.InvalidValueException;
import com.autotune.exceptions.SlaClassNotSupportedException;

import java.util.ArrayList;

/**
 * An ApplicationServiceStack can have multiple layers on different levels.
 */
public class ApplicationServiceStack
{
    String applicationServiceName;
    String namespace;
    String sla_class;
    ArrayList<AutotuneConfig> autotuneConfigs;

    public ApplicationServiceStack(String applicationServiceName, String namespace, String sla_class)
    {
        this.applicationServiceName = applicationServiceName;
        this.namespace = namespace;
        this.sla_class = sla_class;

        autotuneConfigs = new ArrayList<>();
    }

    public String getSla_class() {
        return sla_class;
    }

    public void setSla_class(String sla_class) throws SlaClassNotSupportedException
    {
        if (sla_class != null)
            this.sla_class = sla_class;
        else
            throw new SlaClassNotSupportedException();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) throws InvalidValueException
    {
        if(namespace != null)
            this.namespace = namespace;
        else
            throw new InvalidValueException("Namespace cannot be null");
    }

    public String getApplicationServiceName() {
        return applicationServiceName;
    }

    public void setApplicationServiceName(String applicationServiceName) throws InvalidValueException
    {
        if (applicationServiceName != null)
            this.applicationServiceName = applicationServiceName;
        else
            throw new InvalidValueException("Application service name cannot be null");
    }

    public ArrayList<AutotuneConfig> getStackLayers() {
        return autotuneConfigs;
    }

    public void setStackLayers(ArrayList<AutotuneConfig> autotuneConfigs) {
        this.autotuneConfigs = autotuneConfigs;
    }

    @Override
    public String toString() {
        return "ApplicationTunables{" +
                "applicationName='" + applicationServiceName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", type='" + sla_class + '\'' +
                ", layers=" + autotuneConfigs +
                '}';
    }
}

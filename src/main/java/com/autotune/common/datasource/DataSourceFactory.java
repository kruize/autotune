/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.common.datasource;

import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.TooManyRecursiveCallsException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.exceptions.ServiceNotFound;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import io.fabric8.kubernetes.api.model.Service;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and configures the datasource class for the specified datasource string
 * TODO: this class will be deleted in next cleanup PR and important functions will be moved to DataSourceOperatorImpl class
 */
public class DataSourceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    private DataSourceFactory() {
    }

    public static DataSource getDataSource(String dataSource) throws MonitoringAgentNotFoundException {
        String monitoringAgentEndpoint = null;
        DataSourceInfo datasource = null;

        if (dataSource.toLowerCase().equals(KruizeDeploymentInfo.monitoring_agent))
            monitoringAgentEndpoint = KruizeDeploymentInfo.monitoring_agent_endpoint;

        // Monitoring agent endpoint not set in the configmap
        if (monitoringAgentEndpoint == null || monitoringAgentEndpoint.isEmpty())
            monitoringAgentEndpoint = getServiceEndpoint(KruizeDeploymentInfo.monitoring_service);

        if (dataSource.equals(AnalyzerConstants.PROMETHEUS_DATA_SOURCE))
            return new PrometheusDataSource(monitoringAgentEndpoint);

        LOGGER.error("Datasource " + dataSource + " not supported");
        return null;
    }

    /**
     * Gets the service endpoint for the datasource service through the cluster IP
     * of the service.
     *
     * @return Endpoint of the service.
     * @throws ServiceNotFound
     */
    private static String getServiceEndpoint(String serviceName) {
        //No endpoint was provided in the configmap, find the endpoint from the service.
        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        List<Service> serviceList = kubernetesServices.getServicelist(null);
        kubernetesServices.shutdownClient();
        String serviceEndpoint = null;

        try {
            if (serviceName == null) {
                throw new ServiceNotFound();
            }

            for (Service service : serviceList) {
                String name = service.getMetadata().getName();
                if (name.toLowerCase().equals(serviceName)) {
                    String clusterIP = service.getSpec().getClusterIP();
                    int port = service.getSpec().getPorts().get(0).getPort();
                    LOGGER.debug(KruizeDeploymentInfo.cluster_type);
                    if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.MINIKUBE)) {
                        serviceEndpoint = AnalyzerConstants.HTTP_PROTOCOL + "://" + clusterIP + ":" + port;
                    }
                    if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.OPENSHIFT)) {
                        serviceEndpoint = AnalyzerConstants.HTTPS_PROTOCOL + "://" + clusterIP + ":" + port;
                    }
                }
            }
        } catch (ServiceNotFound e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.SERVICE_NOT_FOUND);
        }

        if (serviceEndpoint == null) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.ENDPOINT_NOT_FOUND);
        }

        return serviceEndpoint;
    }

    /**
     * @param jsonObj The JSON that needs to be parsed
     * @param key     The key to search in the JSON
     * @param values  ArrayList to hold the key values in the JSON
     * @param level   Level of recursion
     */
    static void parseJsonForKey(JSONObject jsonObj, String key, ArrayList<String> values, int level) throws TooManyRecursiveCallsException {
        level += 1;

        if (level > 30)
            throw new TooManyRecursiveCallsException();

        for (String keyStr : jsonObj.keySet()) {
            Object keyvalue = jsonObj.get(keyStr);

            if (keyStr.equals(key))
                values.add(keyvalue.toString());

            //for nested objects
            if (keyvalue instanceof JSONObject)
                parseJsonForKey((JSONObject) keyvalue, key, values, level);

            //for json array, iterate and recursively get values
            if (keyvalue instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) keyvalue;
                for (int index = 0; index < jsonArray.length(); index++) {
                    Object jsonObject = jsonArray.get(index);
                    if (jsonObject instanceof JSONObject) {
                        parseJsonForKey((JSONObject) jsonObject, key, values, level);
                    }
                }
            }
        }
    }
}

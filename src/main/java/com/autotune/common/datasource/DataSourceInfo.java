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
package com.autotune.common.datasource;

import java.net.MalformedURLException;
import java.net.URL;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.slf4j.LoggerFactory;
/**
 * This DataSourceInfo object is used to store information about metric collectors like Prometheus, LogicMonitor, Dynatrace, Amazon Timestream etc
 * Example
 * "datasource": {
 *     "name": "prometheus-1",
 *     "provider": "prometheus",
 *     "serviceName": "prometheus-k8s",
 *     "namespace": "monitoring",
 *     "url": ""
 * }
 */
public class DataSourceInfo {
    private final String name;
    private final String provider;
    private final String serviceName;
    private final String namespace;
    private final URL url;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSourceInfo.class);

    public DataSourceInfo(String name, String provider, String serviceName, String namespace, URL url) {
        this.name = name;
        this.provider = provider;
        if (null == url) {
            this.url = getDNSBasedUrlForService(serviceName, namespace, provider);
        } else {
            this.url = url;
        }
        this.serviceName = serviceName;
        this.namespace = namespace;
    }

    /**
     * Returns the name of the data source
     * @return String containing the name of the data source
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the provider type of the data source
     * @return String containing the provider of the data source
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the serviceName of the data source
     * @return String containing the name of service for the data source
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the namespace in which data source service is deployed
     * @return String containing the namespace of service for the data source
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the URL of the data source
     * @return URL containing the URL of the data source
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Returns the DNS Based URL for accessing the service
     * @return URL containing the URL of the data source
     */
    private URL getDNSBasedUrlForService(String serviceName, String namespace, String provider) {
        URL dnsUrl = null;
        try {
            String url = "";
            DataSourceOperator op = DataSourceOperatorImpl.getInstance();
            String servicePort = op.getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS).getDefaultServicePortForProvider();

            if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.MINIKUBE)) {
                url  = AnalyzerConstants.HTTP_PROTOCOL + "://" + serviceName + "." + namespace + KruizeConstants.DataSourceConstants.SERVICE_DNS + ":" + servicePort;
            } else if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.OPENSHIFT)) {
                url = AnalyzerConstants.HTTPS_PROTOCOL + "://" + serviceName + "." + namespace + KruizeConstants.DataSourceConstants.SERVICE_DNS + ":" + servicePort;
            }
            dnsUrl = new URL(url);
        } catch (MalformedURLException e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.INVALID_DATASOURCE_URL);
        }
        return dnsUrl;
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" +
                "name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", url=" + url +
                '}';
    }
}

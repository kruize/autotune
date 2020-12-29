package com.autotune.dependencyAnalyzer.datasource;

import com.autotune.exceptions.MonitoringAgentNotFoundException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class DataSourceFactory
{
    public static DataSource getDataSource(String dataSource) throws MonitoringAgentNotFoundException
    {
        String monitoringAgentEndpoint = getMonitoringAgentEndpoint();
        String token = System.getenv("auth_token");

        if (dataSource.equals("prometheus"))
        {
            return new PrometheusDataSource(monitoringAgentEndpoint, token);
        }

        return null;
    }

    private static String getMonitoringAgentEndpoint() throws MonitoringAgentNotFoundException
    {
        String monitoringAgentEndpoint = System.getenv("monitoring_agent_endpoint");
        if (monitoringAgentEndpoint != null)
            return monitoringAgentEndpoint;

        //No URL was provided, find the endpoint from the service.
        KubernetesClient client = new DefaultKubernetesClient();
        ServiceList serviceList = client.services().inAnyNamespace().list();
        String monitoringAgentService = System.getenv("monitoring_service");

        if (monitoringAgentService == null)
            throw new MonitoringAgentNotFoundException();

        for (Service service : serviceList.getItems())
        {
            String serviceName = service.getMetadata().getName();
            if (serviceName.toLowerCase().equals(monitoringAgentService))
            {
                System.out.println(serviceName);
                String clusterIP = service.getSpec().getClusterIP();
                int port = service.getSpec().getPorts().get(0).getPort();
                return "http://" + clusterIP + ":" + port;
            }
        }
        return null;
    }
}

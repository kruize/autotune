package com.autotune.common.datasource;

import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.TooManyRecursiveCallsException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.auth.AuthenticationConfig;
import com.autotune.common.auth.AuthenticationStrategy;
import com.autotune.common.auth.AuthenticationStrategyFactory;
import com.autotune.common.datasource.prometheus.PrometheusDataOperatorImpl;
import com.autotune.common.exceptions.datasource.ServiceNotFound;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import io.fabric8.kubernetes.api.model.Service;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DataSourceOperatorImpl implements DataSourceOperator {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSourceOperatorImpl.class);
    private static DataSourceOperatorImpl dataSourceOperator = null;
    protected DataSourceOperatorImpl() {
    }

    /**
     * Returns the instance of DataSourceOperatorImpl class
     * @return DataSourceOperatorImpl instance
     */
    public static DataSourceOperatorImpl getInstance() {
        if (null == dataSourceOperator) {
            dataSourceOperator = new DataSourceOperatorImpl();
        }
        return dataSourceOperator;
    }

    /**
     * Returns the instance of specific operator class based on provider type
     * @param provider String containg the name of provider
     * @return instance of specific operator
     */
    @Override
    public DataSourceOperatorImpl getOperator(String provider) {
        if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            return PrometheusDataOperatorImpl.getInstance();
        }
        return null;
    }

    /**
     * Returns the default service port for prometheus
     * @return String containing the port number
     */
    @Override
    public String getDefaultServicePortForProvider() {
        return "";
    }

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     *
     * @param dataSourceUrl         String containing the url for the datasource
     * @param authenticationConfig
     * @return DatasourceReachabilityStatus
     */
    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl, AuthenticationConfig authenticationConfig) {
        return null;
    }

    /**
     * executes specified query on datasource and returns the result value
     *
     * @param url                   String containing the url for the datasource
     * @param query                 String containing the query to be executed
     * @param authenticationConfig
     * @return Object containing the result value for the specified query
     */
    @Override
    public Object getValueForQuery(String url, String query, AuthenticationConfig authenticationConfig) {
        return null;
    }

    /**
     * returns query endpoint for datasource
     * @return String containing query endpoint
     */
    @Override
    public String getQueryEndpoint() {
        return null;
    }
    /**
     * executes specified query on datasource and returns the JSON Object
     *
     * @param url                   String containing the url for the datasource
     * @param query                 String containing the query to be executed
     * @param authenticationConfig
     * @return JSONObject for the specified query
     */
    @Override
    public JSONObject getJsonObjectForQuery(String url, String query, AuthenticationConfig authenticationConfig) {
        return null;
    }

    /**
     * executes specified query on datasource and returns the result array
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JsonArray containing the result array for the specified query
     */
    @Override
    public JsonArray getResultArrayForQuery(String url, String query, AuthenticationConfig authenticationConfig) {
        return null;
    }

    /**
     * Validates a JSON array to ensure it is not null, not a JSON null, and has at least one element.
     *
     * @param resultArray The JSON array to be validated.
     * @return True if the JSON array is valid (not null, not a JSON null, and has at least one element), otherwise false.
     */
    @Override
    public boolean validateResultArray(JsonArray resultArray) { return false;}

    /**
     * TODO: To find a suitable place for this function later
     * returns authentication token for datasource
     * @return String containing token
     */
    public String getToken() throws IOException {
        String fileName = KruizeConstants.AUTH_MOUNT_PATH+"token";
        String authToken = new String(Files.readAllBytes(Paths.get(fileName)));
        return authToken;
    }

    /**
     * TODO: To find a suitable place for this function later
     * Run the getAppsForLayer and return the list of applications matching the layer.
     * @param dataSource
     * @param query getAppsForLayer query for the layer
     * @param key The key to search for in the response
     * @return ArrayList of all applications from the query
     * @throws MalformedURLException
     */
    public ArrayList<String> getAppsForLayer(DataSourceInfo dataSource, String query, String key) throws MalformedURLException {
        String dataSourceURL = dataSource.getUrl().toString();
        String provider = dataSource.getProvider();
        DataSourceOperator op = this.getOperator(provider);
        String queryEndpoint = op.getQueryEndpoint();
        String response = null;
        ArrayList<String> valuesList = new ArrayList<>();
        String queryURL = dataSourceURL + queryEndpoint + query;
        LOGGER.debug("Query URL is: {}", queryURL);
        try {
            AuthenticationConfig authenticationConfig = dataSource.getAuthenticationConfig();
            AuthenticationStrategy authenticationStrategy = AuthenticationStrategyFactory.createAuthenticationStrategy(authenticationConfig);
            // Create the client
            GenericRestApiClient genericRestApiClient = new GenericRestApiClient(authenticationStrategy);
            genericRestApiClient.setBaseURL(dataSourceURL + queryEndpoint);
            JSONObject responseJson = genericRestApiClient.fetchMetricsJson("GET", query);
            int level = 0;
            try {
                parseJsonForKey(responseJson, key, valuesList, level);
                LOGGER.debug("Applications for the query: {}", valuesList.toString());
            } catch (TooManyRecursiveCallsException e) {
                e.printStackTrace();
            }
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            LOGGER.error("Unable to proceed due to invalid connection to URL: "+ queryURL);
        }
        return valuesList;
    }

    /**
     * TODO: monitoring agent will be replaced by default datasource later
     * returns DataSourceInfo objects for default datasource which is currently monitoring agent
     * @return DataSourceInfo objects
     */
    public static DataSourceInfo getMonitoringAgent(String dataSource) throws MonitoringAgentNotFoundException, MalformedURLException {
        String monitoringAgentEndpoint;
        DataSourceInfo monitoringAgent = null;

        if (dataSource.toLowerCase().equals(KruizeDeploymentInfo.monitoring_agent)) {
            monitoringAgentEndpoint = KruizeDeploymentInfo.monitoring_agent_endpoint;
            // Monitoring agent endpoint not set in the configmap
            if (monitoringAgentEndpoint == null || monitoringAgentEndpoint.isEmpty()) {
                monitoringAgentEndpoint = getServiceEndpoint(KruizeDeploymentInfo.monitoring_service);
            }
            if (dataSource.equals(AnalyzerConstants.PROMETHEUS_DATA_SOURCE)) {
                AuthenticationConfig authenticationConfig = AuthenticationConfig.noAuth();
                monitoringAgent = new DataSourceInfo(KruizeDeploymentInfo.monitoring_agent, AnalyzerConstants.PROMETHEUS_DATA_SOURCE, null, null, new URL(monitoringAgentEndpoint), authenticationConfig);
            }
        }

        if (monitoringAgent == null) {
            LOGGER.error("Datasource " + dataSource + " not supported");
        }

        return monitoringAgent;
    }

    /**
     * TODO: To find a suitable place for this function later
     * Gets the service endpoint for the datasource service through the cluster IP
     * of the service.
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
     * TODO: To find a suitable place for this function later
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

package com.autotune.UserInterfaceSupport.services;

import com.autotune.UserInterfaceSupport.util.UISMUtils;
import com.autotune.utils.AutotuneConstants;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

public class GetDeployments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDeployments.class);

    /**
     * This API returns the list of deployments available in the namespace
     *
     * Sample response body of the API
     * {
     *   "uism_version" : "v0.1",
     *   "status": "success",
     *   "data": {
     *     "namespace": "default",
     *     "deployments" : [
     *       "petclinic",
     *       "techempower"
     *     ]
     *   }
     * }
     *
     * @param request
     * @param response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            JSONObject inputJson = new JSONObject(inputData);
            if (!inputJson.has(AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else if (null == inputJson.getString(AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE)
                    || inputJson.getString(AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE).isBlank()
                    || inputJson.getString(AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE).isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                String namespace = inputJson.getString(AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE);
                if (UISMUtils.checkIfNamespaceExists(namespace)) {
                    JSONObject returnJson = new JSONObject();
                    JSONObject dataJson = new JSONObject();
                    JSONArray deploymentsList = new JSONArray();
                    KubernetesClient client = new DefaultKubernetesClient();
                    client.apps().deployments().inNamespace(namespace).list().getItems().forEach(deployment -> {
                        deploymentsList.put(deployment.getMetadata().getName());
                    });
                    dataJson.put(
                            AutotuneConstants.UISMConstants.UISMJsonKeys.DEPLOYMENTS,
                            deploymentsList
                    );
                    dataJson.put(
                            AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACE,
                            namespace
                    );
                    returnJson.put(
                            AutotuneConstants.UISMConstants.UISMJsonKeys.UISM_VERSION,
                            AutotuneConstants.UISMConstants.UISMDefaults.UISM_VERSION
                    );
                    returnJson.put(
                            AutotuneConstants.UISMConstants.UISMJsonKeys.STATUS,
                            AutotuneConstants.UISMConstants.UISMStandard.SUCCESS_STATUS
                    );
                    returnJson.put(
                            AutotuneConstants.UISMConstants.UISMJsonKeys.DATA,
                            dataJson
                    );
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(returnJson.toString(4));
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            LOGGER.error("{}", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        doPost(request, response);
    }
}

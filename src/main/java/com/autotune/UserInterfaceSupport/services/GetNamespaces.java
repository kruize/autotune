package com.autotune.UserInterfaceSupport.services;

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
import java.io.IOException;

public class GetNamespaces extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetNamespaces.class);

    /**
     * This API returns the list of namespaces available in the cluster
     *
     * Sample response body of the API
     * {
     *   "uism_version" : "v0.1",
     *   "status": "success",
     *   "data": {
     *     "namespaces" : [
     *       "default",
     *       "monitoring"
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
            JSONObject returnJson = new JSONObject();
            JSONObject dataJson = new JSONObject();
            JSONArray namespacesList = new JSONArray();
            KubernetesClient client = new DefaultKubernetesClient();
            client.namespaces().list().getItems().forEach(namespace -> {
                namespacesList.put(namespace.getMetadata().getName());
            });
            dataJson.put(
                    AutotuneConstants.UISMConstants.UISMJsonKeys.NAMESPACES,
                    namespacesList
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

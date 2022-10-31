package com.autotune.analyzer.services;

import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ListNamespaces extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        KubernetesServices kubernetesServices = null;
        try {
            // Initialising the kubernetes service
            kubernetesServices = new KubernetesServicesImpl();
            // Set response headers
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "POST, GET");
            response.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
            response.addHeader("Access-Control-Max-Age", "1728000");
            JSONObject returnJson = new JSONObject();
            JSONObject dataJson = new JSONObject();
            JSONArray namespacesList = new JSONArray();
            kubernetesServices.getNamespaces().forEach(namespace -> {
                namespacesList.put(namespace.getMetadata().getName());
            });
            dataJson.put(AutotuneConstants.JSONKeys.NAMESPACES, namespacesList);
            returnJson.put(AutotuneConstants.JSONKeys.DATA, dataJson);
            // Set content type
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(returnJson.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (kubernetesServices != null) {
                kubernetesServices.shutdownClient();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
}

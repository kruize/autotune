/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.services;

import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

public class ListDeploymentsInNamespace extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        KubernetesServices kubernetesServices = null;
        try {
            // Set content type
            response.setContentType("application/json");
            // Set encoding
            response.setCharacterEncoding("UTF-8");
            // Check if the namespace is passed as a URL param
            String namespace = request.getParameter(KruizeConstants.JSONKeys.NAMESPACE);
            String error = null;
            if (null == namespace) {
                // Check if the request has a JSON body in which namespace is passed
                String inputData = request.getReader().lines().collect(Collectors.joining());
                JSONObject inputJson = new JSONObject(inputData);
                if (!inputJson.has(KruizeConstants.JSONKeys.NAMESPACE)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    error = KruizeConstants.ErrorMsgs.APIErrorMsgs.ListDeploymentsInNamespace.NO_NAMESPACE_SENT;
                    namespace = null;
                } else {
                    if (null == inputJson.getString(KruizeConstants.JSONKeys.NAMESPACE)) {
                        error = KruizeConstants.ErrorMsgs.APIErrorMsgs.ListDeploymentsInNamespace.NO_NAMESPACE_SENT;
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        namespace = null;
                    } else if( inputJson.getString(KruizeConstants.JSONKeys.NAMESPACE).isBlank()
                            || inputJson.getString(KruizeConstants.JSONKeys.NAMESPACE).isEmpty()) {
                        error = KruizeConstants.ErrorMsgs.APIErrorMsgs.ListDeploymentsInNamespace.EMPTY_NAMESPACE;
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        namespace = null;
                    } else {
                        namespace = inputJson.getString(KruizeConstants.JSONKeys.NAMESPACE);
                    }
                }
            }
            // If namespace is not found return error
            if (null == namespace) {
                // if error is not set, set it to invalid namespace
                if (null == error) {
                    error = KruizeConstants.ErrorMsgs.APIErrorMsgs.ListDeploymentsInNamespace.INVALID_NAMESPACE;
                }
                JSONObject returnJson = new JSONObject();
                returnJson.put(KruizeConstants.JSONKeys.ERROR, error);
                response.getWriter().println(returnJson.toString(4));
            } else {
                // Initialising the kubernetes service
                kubernetesServices = new KubernetesServicesImpl();
                // Create a return object
                JSONObject returnJson = new JSONObject();
                JSONObject dataJson = new JSONObject();
                JSONArray deploymentsList = new JSONArray();
                kubernetesServices.getDeploymentsBy(namespace).forEach(deployment -> {
                    deploymentsList.put(deployment.getMetadata().getName());
                });
                dataJson.put(KruizeConstants.JSONKeys.DEPLOYMENTS, deploymentsList);
                returnJson.put(KruizeConstants.JSONKeys.DATA, dataJson);
                // Return OK
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(returnJson.toString(4));
            }
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

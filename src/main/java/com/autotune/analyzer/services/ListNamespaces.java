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
import com.autotune.utils.AutotuneConstants;
import com.autotune.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ListNamespaces extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        // Adding CORS headers as this API is accessed by UI
        Utils.addCORSHeaders(response);
        KubernetesServices kubernetesServices = null;
        try {
            // Initialising the kubernetes service
            kubernetesServices = new KubernetesServicesImpl();
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

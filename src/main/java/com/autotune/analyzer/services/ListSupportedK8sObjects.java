/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.serviceObjects.ListSupportedK8sObjectsSO;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebServlet(asyncSupported = true)
public class ListSupportedK8sObjects extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListSupportedK8sObjects.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Create the Service Object for the List Supported K8S Objects endpoint
        ListSupportedK8sObjectsSO listSupportedK8sObjectsSO =  new ListSupportedK8sObjectsSO();
        for (String k8sObjectTypeString : getSupportedK8sObjects()) {
            listSupportedK8sObjectsSO.addSupportedK8sObject(k8sObjectTypeString);
        }
        String responseGSONString = "";
        // Create a GSON builder
        Gson gsonObj = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                .create();
        // Convert the Service object to JSON
        responseGSONString = gsonObj.toJson(listSupportedK8sObjectsSO);

        // Write the JSON to Response and close the writer
        response.getWriter().println(responseGSONString);
        response.getWriter().close();
    }

    /**
     * Returns the Kubernetes object types a client can submit to createExperiment.
     * This applies the same check as the remote monitoring createExperiment validation
     * (Utils.getApproriateK8sObjectType). It deliberately does not use UNSUPPORTED_K8S_OBJECT_TYPES,
     * because that list also holds types which are only excluded from local monitoring metadata discovery
     * (e.g. DeploymentConfig) but are still valid for remote experiments.
     */
    static List<String> getSupportedK8sObjects() {
        List<String> supported = new ArrayList<>();
        for (AnalyzerConstants.K8S_OBJECT_TYPES k8SObjectType : AnalyzerConstants.K8S_OBJECT_TYPES.values()) {
            String k8sObjectTypeString = Utils.getAppropriateK8sObjectTypeString(k8SObjectType);
            if (null != k8sObjectTypeString && null != Utils.getApproriateK8sObjectType(k8sObjectTypeString)) {
                supported.add(k8sObjectTypeString);
            }
        }
        return supported;
    }

    // Redirect the Get request to the POST handler
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}

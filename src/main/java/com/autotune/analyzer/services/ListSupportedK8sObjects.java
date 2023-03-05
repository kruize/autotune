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

import com.autotune.analyzer.serviceObjects.ListSupportedK8sObjectsSO;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.utils.AnalyzerConstants;
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
import java.util.Date;

@WebServlet(asyncSupported = true)
public class ListSupportedK8sObjects extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListSupportedK8sObjects.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Create the Service Object for the List Supported K8S Objects endpoint
        ListSupportedK8sObjectsSO listSupportedK8sObjectsSO =  new ListSupportedK8sObjectsSO();
        // Iterate over the supported Kubernetes Object type supported by Kruize
        for (AnalyzerConstants.K8S_OBJECT_TYPES k8SObjectType : AnalyzerConstants.K8S_OBJECT_TYPES.values()) {
            // Get the String value of the K8S object types
            String k8sObjectTypeString = Utils.getAppropriateK8sObjectTypeString(k8SObjectType);
            // Check for null and if it's not null add to the list of K8S objects in the Service Object
            if (null != k8sObjectTypeString) {
                listSupportedK8sObjectsSO.addSupportedK8sObject(k8sObjectTypeString);
            }
        }
        String responseGSONString = "";
        // Create a GSON builder
        Gson gsonObj = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .create();
        // Convert the Service object to JSON
        responseGSONString = gsonObj.toJson(listSupportedK8sObjectsSO);

        // Write the JSON to Response and close the writer
        response.getWriter().println(responseGSONString);
        response.getWriter().close();
    }

    // Redirect the Get request to the POST handler
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}

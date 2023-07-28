/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer;

import com.autotune.analyzer.experiment.Experimentator;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.services.*;
import com.autotune.operator.KruizeOperator;
import com.autotune.utils.ServerContext;
import org.eclipse.jetty.servlet.ServletContextHandler;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;


import java.io.IOException;
import java.util.List;


public class Analyzer {
    public static void start(ServletContextHandler contextHandler) {

        Experimentator.start();
        KruizeOperator kruizeOperator = new KruizeOperator();

        try {
            addServlets(contextHandler);
            PerformanceProfilesDeployment.getPerformanceProfiles(); //  Performance profile should be called first
            KruizeOperator.getKruizeObjects(kruizeOperator);
            PrintJaegerDependecies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void PrintJaegerDependecies(){
        long currentTimestamp = System.currentTimeMillis();


        String jaegerServiceName = "jaeger"; // Replace with your Jaeger service name
        String jaegerNamespace = "default"; // Replace with the namespace of your Jaeger service

        String apiUrl = "http://" + jaegerServiceName + "." + jaegerNamespace + ".svc.cluster.local:16686/api/dependencies?endTs=" + currentTimestamp + "&lookback=604800000";

        System.out.println("API URL: " + apiUrl);
        try {
            URI uri = new URI(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();

                System.out.println("----------------Jaeger Dependencies---------------------------------");
                System.out.println(response.toString());
            } else {
                System.out.println("Error: Failed to fetch data from the API. Response code: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    public static void addServlets(ServletContextHandler context) {
        context.addServlet(ListStacks.class, ServerContext.LIST_STACKS);
        context.addServlet(ListStackLayers.class, ServerContext.LIST_STACK_LAYERS);
        context.addServlet(ListStackTunables.class, ServerContext.LIST_STACK_TUNABLES);
        context.addServlet(ListKruizeTunables.class, ServerContext.LIST_KRUIZE_TUNABLES);
        context.addServlet(SearchSpace.class, ServerContext.SEARCH_SPACE);
        context.addServlet(ListExperiments.class, ServerContext.LIST_EXPERIMENTS);
        context.addServlet(ExperimentsSummary.class, ServerContext.EXPERIMENTS_SUMMARY);
        context.addServlet(CreateExperiment.class, ServerContext.CREATE_EXPERIMENT);
        context.addServlet(UpdateResults.class, ServerContext.UPDATE_RESULTS);
        context.addServlet(ListRecommendations.class, ServerContext.RECOMMEND_RESULTS);
        context.addServlet(PerformanceProfileService.class, ServerContext.CREATE_PERF_PROFILE);
        context.addServlet(PerformanceProfileService.class, ServerContext.LIST_PERF_PROFILES);

        // Adding UI support API's
        context.addServlet(ListNamespaces.class, ServerContext.LIST_NAMESPACES);
        context.addServlet(ListDeploymentsInNamespace.class, ServerContext.LIST_DEPLOYMENTS);
        context.addServlet(ListSupportedK8sObjects.class, ServerContext.LIST_K8S_OBJECTS);
    }
}

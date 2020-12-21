/*******************************************************************************
 * Copyright (c) 2020 Red Hat, IBM Corporation and others.
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
package com.autotune.service;

import com.autotune.collection.AutotuneObject;
import com.autotune.collection.CollectAutotuneObjects;
import com.autotune.application.Application;
import com.autotune.collection.AutotuneConfig;
import com.autotune.application.Query;
import com.autotune.application.Tunable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TuningSetService extends HttpServlet
{
    /**
     * Returns the JSON array response containing all the applications along with their tunables for the SLA.
     *
     * Request:
     * `GET /getTunables` gives the tuning set C for all the applications monitored.
     *
     * `GET /getTunables?set=<A|B|C>` gives the tuning set A for all the applications monitored.
     *
     * `GET /getTunables?application_name=<APPLICATION_NAME>` for getting the tuning set C of a specific application.
     *
     * `GET /getTunables?application_name=<APPLICATION_NAME>&type='container'` for getting tunables of a specific type for the application.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        JSONArray outputArray = new JSONArray();
        resp.setContentType("application/json");

        String tuningSet = req.getParameter("set");
        String applicationName = req.getParameter("application_name");
        String level = req.getParameter("level");

        if (tuningSet == null)
            getTuningSetC(outputArray);
        else
        {
            tuningSet = tuningSet.toUpperCase();

            if (tuningSet.equals("A"))
            {
                getTuningSetA(outputArray);
            }
            else if (tuningSet.equals("B"))
            {
                getTuningSetB(outputArray);
            }
            else
            {
                getTuningSetC(outputArray);
            }
        }

        resp.getWriter().println(outputArray.toString(4));
    }

    /**
     * Contains list of services matched to the list of autotune objects in the cluster.
     *
     * Example JSON:
     * [
     *   {
     *     "name": "petclinic-autotune",
     *     "namespace": "default",
     *     "sla_class": "response_time",
     *     "services": [
     *       "petclinic-deployment-6d4c8678d4-jmz8x"
     *     ],
     *     "direction": "lower"
     *   }
     * ]
     * @param outputArray
     */
    private void getTuningSetA(JSONArray outputArray)
    {
        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneObjectList) {
            JSONObject autotuneObjectJson = new JSONObject();
            autotuneObjectJson.put("name", autotuneObject.getName());
            autotuneObjectJson.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());
            autotuneObjectJson.put("direction", autotuneObject.getSlaInfo().getDirection());
            autotuneObjectJson.put("namespace", autotuneObject.getNamespace());

            JSONArray applicationArray = new JSONArray();

            for (String application : autotuneObject.applicationsMap.keySet()) {
                applicationArray.put(application);
            }

            autotuneObjectJson.put("services", applicationArray);
            outputArray.put(autotuneObjectJson);
        }
    }

    /**
     * Contains the list of applications, along with the layers detected in the application.
     *
     * Example JSON:
     * [
     *   {
     *     "name": "petclinic-autotune",
     *     "namespace": "default",
     *     "sla_class": "response_time",
     *     "services": [
     *       {
     *         "name": "petclinic-deployment-6d4c8678d4-jmz8x",
     *         "layers": [
     *           {
     *             "level": 0,
     *             "name": "container",
     *             "details": "generic container tunables"
     *           },
     *           {
     *             "level": 1,
     *             "name": "openj9",
     *             "details": "java openj9 tunables"
     *           }
     *         ]
     *       }
     *     ],
     *     "direction": "lower"
     *   }
     * ]
     *
     * @param outputArray
     */
    private void getTuningSetB(JSONArray outputArray)
    {
        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneObjectList)
        {
            JSONObject autotuneObjectJson = new JSONObject();
            autotuneObjectJson.put("name", autotuneObject.getName());
            autotuneObjectJson.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());
            autotuneObjectJson.put("direction", autotuneObject.getSlaInfo().getDirection());
            autotuneObjectJson.put("namespace", autotuneObject.getNamespace());

            JSONArray applicationTunableArray = new JSONArray();
            for (String application : autotuneObject.applicationsMap.keySet())
            {
                JSONObject applicationJson = new JSONObject();
                applicationJson.put("name", application);

                JSONArray autotuneConfigArray = new JSONArray();

                for (AutotuneConfig autotuneConfig : autotuneObject.applicationsMap.get(application).getLayers())
                {
                    JSONObject autotuneConfigJson = new JSONObject();
                    autotuneConfigJson.put("name", autotuneConfig.getName());
                    autotuneConfigJson.put("details", autotuneConfig.getDetails());
                    autotuneConfigJson.put("level", autotuneConfig.getLevel());

                    autotuneConfigArray.put(autotuneConfigJson);
                }

                applicationJson.put("layers", autotuneConfigArray);
                applicationTunableArray.put(applicationJson);
            }
            autotuneObjectJson.put("services", applicationTunableArray);
            outputArray.put(autotuneObjectJson);
        }
    }

    /**
     * Contains the list of applications monitored by autotune, along with in-depth information
     * about the layers detected, and queries used.
     *
     * Example JSON:
     * [
     *   {
     *     "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
     *     "namespace": "default",
     *     "type": "response_time",
     *     "application_tunables": [
     *       {
     *         "level": 0,
     *         "layer_tunables": [
     *           {
     *             "value_type": "double",
     *             "query": "container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}",
     *             "tunables": [
     *               {
     *                 "lower_bound": "150M",
     *                 "name": "memoryLimit",
     *                 "upper_bound": "300M"
     *               },
     *               {
     *                 "lower_bound": "150M",
     *                 "name": "memoryRequests",
     *                 "upper_bound": "300M"
     *               }
     *             ],
     *             "details": "Current RSS value"
     *           },
     *           {
     *             "value_type": "double",
     *             "query": "(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])",
     *             "tunables": [
     *               {
     *                 "lower_bound": "2.0",
     *                 "name": "cpuLimit",
     *                 "upper_bound": "4.0"
     *               },
     *               {
     *                 "lower_bound": "1.0",
     *                 "name": "cpuRequest",
     *                 "upper_bound": "3.0"
     *               }
     *             ],
     *             "details": "Current CPU used"
     *           }
     *         ],
     *         "layer": "container",
     *         "layer_details": "generic container tunables"
     *       },
     *       {
     *         "level": 1,
     *         "layer_tunables": [
     *           {
     *             "value_type": "double",
     *             "query": "jvm_memory_used_bytes{area=\"heap\",id=\"nursery-allocate\", pod=petclinic-deployment-6d4c8678d4-jmz8x}",
     *             "tunables": [
     *               {
     *                 "lower_bound": "100M",
     *                 "name": "javaHeap",
     *                 "upper_bound": "250M"
     *               }
     *             ],
     *             "details": "Current Nursery Heap"
     *           }
     *         ],
     *         "layer": "openj9",
     *         "layer_details": "java openj9 tunables"
     *       }
     *     ]
     *   }
     * ]
     *
     * @param outputArray
     */
    private void getTuningSetC(JSONArray outputArray)
    {
        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneObjectList)
        {
            for (String application : autotuneObject.applicationsMap.keySet())
            {
                Application applicationTunables = autotuneObject.applicationsMap.get(application);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("application_name", application);
                jsonObject.put("namespace", applicationTunables.getNamespace());
                jsonObject.put("type", applicationTunables.getType());

                JSONArray applicationTunableArray = new JSONArray();

                for (AutotuneConfig autotuneConfig : applicationTunables.getLayers())
                {
                    JSONObject layerJson = new JSONObject();
                    layerJson.put("layer", autotuneConfig.getName());
                    layerJson.put("level", autotuneConfig.getLevel());
                    layerJson.put("layer_details", autotuneConfig.getDetails());

                    JSONArray layerTunablesArray = new JSONArray();

                    for (Query query : autotuneConfig.getQueries())
                    {
                        JSONObject queryJson = new JSONObject();
                        queryJson.put("query", query.getQuery());
                        queryJson.put("details", query.getDetails());
                        queryJson.put("value_type", query.getValueType());

                        JSONArray tunablesArray = new JSONArray();

                        for (Tunable tunable : query.getTunables())
                        {
                            JSONObject tunableJSON = new JSONObject();
                            tunableJSON.put("name", tunable.getName());
                            tunableJSON.put("upper_bound", tunable.getUpperBound());
                            tunableJSON.put("lower_bound", tunable.getLowerBound());

                            tunablesArray.put(tunableJSON);
                        }

                        queryJson.put("tunables", tunablesArray);
                        layerTunablesArray.put(queryJson);
                    }
                    layerJson.put("layer_tunables", layerTunablesArray);
                    applicationTunableArray.put(layerJson);
                }
                jsonObject.put("application_tunables", applicationTunableArray);
                outputArray.put(jsonObject);
            }
        }
    }
}

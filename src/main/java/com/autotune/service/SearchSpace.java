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

import com.autotune.collection.AutotuneConfig;
import com.autotune.application.Query;
import com.autotune.application.Tunable;
import com.autotune.collection.AutotuneObject;
import com.autotune.collection.CollectAutotuneObjects;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SearchSpace extends HttpServlet
{
    /**
     * Generates the search space used for the analysis.
     *
     * Request:
     * `GET /searchSpace`
     *
     * Example JSON:
     * [
     *   {
     *     "application": "petclinic-deployment-6d4c8678d4-jmz8x",
     *     "tunables": [
     *       {
     *         "value_type": "double",
     *         "lower_bound": "150M",
     *         "name": "memoryLimit",
     *         "upper_bound": "300M",
     *         "direction": "lower"
     *       },
     *       {
     *         "value_type": "double",
     *         "lower_bound": "150M",
     *         "name": "memoryRequests",
     *         "upper_bound": "300M",
     *         "direction": "lower"
     *       },
     *       {
     *         "value_type": "double",
     *         "lower_bound": "2.0",
     *         "name": "cpuLimit",
     *         "upper_bound": "4.0",
     *         "direction": "lower"
     *       },
     *       {
     *         "value_type": "double",
     *         "lower_bound": "1.0",
     *         "name": "cpuRequest",
     *         "upper_bound": "3.0",
     *         "direction": "lower"
     *       },
     *       {
     *         "value_type": "double",
     *         "lower_bound": "100M",
     *         "name": "javaHeap",
     *         "upper_bound": "250M",
     *         "direction": "lower"
     *       }
     *     ],
     *     "sla_class": "response_time"
     *   }
     * ]
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        JSONArray outputJsonArray = new JSONArray();
        resp.setContentType("application/json");

        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneObjectList)
        {
            for (String application : autotuneObject.applicationsMap.keySet())
            {
                JSONObject applicationJson = new JSONObject();

                applicationJson.put("application", application);
                applicationJson.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());

                JSONArray tunablesJsonArray = new JSONArray();
                for(AutotuneConfig autotuneConfig : autotuneObject.applicationsMap.get(application).getLayers())
                {
                    for (Query query : autotuneConfig.getQueries())
                    {
                        for (Tunable tunable : query.getTunables())
                        {
                            JSONObject tunableJson = new JSONObject();
                            tunableJson.put("name", tunable.getName());
                            tunableJson.put("upper_bound", tunable.getUpperBound());
                            tunableJson.put("lower_bound", tunable.getLowerBound());
                            tunableJson.put("value_type", tunable.getValueType());
                            tunableJson.put("direction", autotuneObject.getSlaInfo().getDirection());

                            tunablesJsonArray.put(tunableJson);
                        }
                    }
                }
                applicationJson.put("tunables", tunablesJsonArray);
                outputJsonArray.put(applicationJson);
            }
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }
}

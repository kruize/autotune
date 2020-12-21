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
import com.autotune.collection.CollectAutotuneObjects;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListTunables extends HttpServlet
{
    /**
     * Get the tunables supported by autotune for the SLA.
     *
     * Request:
     * `GET /listTunables?sla=<SLA>` gives all tunables for the SLA
     *
     * `GET /listTunables?sla=<SLA>&layer=<LAYER>` gives tunables for the SLA and the layer
     *
     * `GET /listTunables?sla=<SLA>&layer_level=<LEVEL>` gives tunables for the SLA and the level type.
     *
     * Example JSON:
     * [
     *   {
     *     "layer_level": 0,
     *     "tunables": [
     *       {
     *         "name": "memoryLimit",
     *         "lower_bound": "150M",
     *         "upper_bound": "300M"
     *       },
     *       {
     *         "name": "memoryRequests",
     *         "lower_bound": "150M",
     *         "upper_bound": "300M"
     *       },
     *       {
     *         "name": "cpuLimit",
     *         "lower_bound": "2.0",
     *         "upper_bound": "4.0"
     *       },
     *       {
     *         "name": "cpuRequest",
     *         "lower_bound": "1.0",
     *         "upper_bound": "3.0"
     *       }
     *     ],
     *     "details": "generic container tunables",
     *     "layer_name": "container"
     *   },
     *   {
     *     "layer_level": 1,
     *     "tunables": [
     *       {
     *         "name": "javaHeap",
     *         "lower_bound": "100M",
     *         "upper_bound": "250M"
     *       }
     *     ],
     *     "details": "java openj9 tunables",
     *     "layer_name": "openj9"
     *   }
     * ]
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray outputJsonArray = new JSONArray();
        resp.setContentType("application/json");

        for (AutotuneConfig autotuneConfig : CollectAutotuneObjects.autotuneConfigList)
        {
            JSONObject configJson = new JSONObject();
            configJson.put("layer_name", autotuneConfig.getName());
            configJson.put("details", autotuneConfig.getDetails());
            configJson.put("layer_level", autotuneConfig.getLevel());

            JSONArray layerTunables = new JSONArray();

            for (Query query : autotuneConfig.getQueries())
            {
                for (Tunable tunable : query.getTunables())
                {
                    JSONObject tunableJson = new JSONObject();
                    tunableJson.put("name", tunable.getName());
                    tunableJson.put("upper_bound", tunable.getUpperBound());
                    tunableJson.put("lower_bound", tunable.getLowerBound());

                    layerTunables.put(tunableJson);
                }
            }

            configJson.put("tunables", layerTunables);
            outputJsonArray.put(configJson);
        }

        resp.getWriter().println(outputJsonArray.toString(4));
    }
}

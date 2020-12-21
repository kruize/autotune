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
import com.autotune.collection.AutotuneConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListApplicationService extends HttpServlet
{
    /**
     * Get the list of applications, along with layer information for the application.
     *
     * Request:
     * `GET /listApplications` gives list of applications monitored by autotune, along with the individual detected layers of the application.
     *
     * Example JSON:
     * [
     *   {
     *     "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
     *     "layers": [
     *       {
     *         "level": 0,
     *         "name": "container",
     *         "details": "generic container tunables"
     *       },
     *       {
     *         "level": 1,
     *         "name": "openj9",
     *         "details": "java openj9 tunables"
     *       }
     *     ],
     *     "type": "response_time"
     *   }
     * ]
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray outputJsonArray = new JSONArray();
        resp.setContentType("application/json");
        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneObjectList)
        {
            for (String key : autotuneObject.applicationsMap.keySet())
            {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("application_name", key);
                jsonObject.put("type", autotuneObject.getSlaInfo().getSlaClass());

                JSONArray layersArray = new JSONArray();
                for (AutotuneConfig autotuneConfig : autotuneObject.applicationsMap.get(key).getLayers())
                {
                    JSONObject layerJson = new JSONObject();
                    layerJson.put("name", autotuneConfig.getName());
                    layerJson.put("details", autotuneConfig.getDetails());
                    layerJson.put("level", autotuneConfig.getLevel());
                    layersArray.put(layerJson);
                }
                jsonObject.put("layers", layersArray);
                outputJsonArray.put(jsonObject);
            }
        }

        resp.getWriter().println(outputJsonArray.toString(4));
    }

}

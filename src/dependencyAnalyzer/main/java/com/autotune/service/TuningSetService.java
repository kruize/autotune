package com.autotune.service;

import com.autotune.collection.AutotuneObject;
import com.autotune.collection.CollectAutotuneObjects;
import com.autotune.query.ApplicationTunables;
import com.autotune.query.Layer;
import com.autotune.query.Query;
import com.autotune.tunables.Tunable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TuningSetService extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        JSONArray outputArray = new JSONArray();
        resp.setContentType("application/json");

        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneInfoList)
        {
            for (String application : autotuneObject.applicationTunablesMap.keySet())
            {
                ApplicationTunables applicationTunables = autotuneObject.applicationTunablesMap.get(application);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("application_name", application);
                jsonObject.put("namespace", applicationTunables.getNamespace());
                jsonObject.put("type", applicationTunables.getType());

                JSONArray applicationTunableArray = new JSONArray();

                for (Layer layer : applicationTunables.getLayers())
                {
                    JSONObject layerJson = new JSONObject();
                    layerJson.put("layer", layer.getName());
                    layerJson.put("level", layer.getLevel());
                    layerJson.put("layer_details", layer.getDetails());

                    JSONArray layerTunablesArray = new JSONArray();

                    for (Query query : layer.getQueries())
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
            resp.getWriter().println(outputArray.toString(4));
        }
    }
}

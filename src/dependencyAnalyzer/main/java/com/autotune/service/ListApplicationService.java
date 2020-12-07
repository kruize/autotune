package com.autotune.service;

import com.autotune.collection.AutotuneObject;
import com.autotune.collection.CollectAutotuneObjects;
import com.autotune.query.Layer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListApplicationService extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray outputJsonArray = new JSONArray();
        resp.setContentType("application/json");
        for (AutotuneObject autotuneObject : CollectAutotuneObjects.autotuneInfoList)
        {
            for (String key : autotuneObject.applicationTunablesMap.keySet())
            {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("application_name", key);
                jsonObject.put("type", autotuneObject.getSlaInfo().getSlaName());

                JSONArray layersArray = new JSONArray();
                for (Layer layer : autotuneObject.applicationTunablesMap.get(key).getLayers())
                {
                    JSONObject layerJson = new JSONObject();
                    layerJson.put("name", layer.getName());
                    layerJson.put("details", layer.getDetails());
                    layerJson.put("level", layer.getLevel());
                    layersArray.put(layerJson);
                }
                jsonObject.put("layers", layersArray);
                outputJsonArray.put(jsonObject);
            }
        }

        resp.getWriter().println(outputJsonArray.toString(4));
    }

}

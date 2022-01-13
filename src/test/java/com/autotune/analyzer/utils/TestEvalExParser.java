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
 *
 *******************************************************************************/

package com.autotune.analyzer.utils;

import com.autotune.analyzer.k8sObjects.Metric;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEvalExParser {

    List<String> objFunctionsList = new ArrayList<>(List.of(
            "(( throughput / transaction_response_time) /  max_response_time) * 100",
            "request_sum/request_count",
            "(1.25 * request_count) - (1.5 * (request_sum / request_count)) - (0.25 * request_max)",
            "((request_count / (request_sum / request_count)) / request_max) * 100"
    ));
    @Test
    public void testValidate(){

    for(String objFunction : objFunctionsList) {
        ArrayList<Metric> functionVariables = getFunctionVariables();
        assertEquals(true, new EvalExParser().validate(objFunction, functionVariables));
        }
    }

    private ArrayList<Metric> getFunctionVariables() {

        String sloJson = "{\"function_variables\":[{\"name\":\"request_sum\",\"query\":\"rate(http_server_requests_seconds_sum{method=\\\"GET\\\",outcome=\\\"SUCCESS\\\",status=\\\"200\\\",uri=\\\"/db\\\",}[1m])\",\"datasource\":\"prometheus\",\"value_type\":\"double\"},{\"name\":\"request_count\",\"query\":\"rate(http_server_requests_seconds_count{method=\\\"GET\\\",outcome=\\\"SUCCESS\\\",status=\\\"200\\\",uri=\\\"/db\\\",}[1m])\",\"datasource\":\"prometheus\",\"value_type\":\"double\"},{\"name\":\"request_max\",\"query\":\"http_server_requests_seconds_max{method=\\\"GET\\\",outcome=\\\"SUCCESS\\\",status=\\\"200\\\",uri=\\\"/db\\\"}\",\"datasource\":\"prometheus\",\"value_type\":\"double\"}]}";
        JSONObject functionVariablesObject = new JSONObject(sloJson.toString());
        JSONArray functionVariables = functionVariablesObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);

        ArrayList<Metric> metricArrayList = new ArrayList<>();

        for (Object functionVariableObj : functionVariables) {
            JSONObject functionVariableJson = (JSONObject) functionVariableObj;
            String variableName = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);
            String query = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.QUERY);
            String datasource = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
            String valueType = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);

            Metric metric = new Metric(variableName,
                    query,
                    datasource,
                    valueType);

            metricArrayList.add(metric);
        }
        return metricArrayList;
    }

    @Test
    public void testParse(){

        for(String objFunction : objFunctionsList) {
//            Map<String, String> objFunctionMap = getObjFunctionMapValues(objFunction);
            Map<String, String> objFunctionMap = new HashMap<>();
            assertEquals("objective_function_map is missing or empty\n", new EvalExParser().parse(objFunction, "String", objFunctionMap));
//            assertEquals("some-result", new EvalExParser().parse(objFunction, "String", objFunctionMap));
        }
    }

    private Map<String, String> getObjFunctionMapValues(String objFunction) {
        Map<String, String> objFunctionMap = new HashMap<>();

        objFunctionMap.put("throughput","8942");
        objFunctionMap.put("transaction_response_time","0.71");
        objFunctionMap.put("max_response_time","719.408857");
        objFunctionMap.put("request_sum","719.408857");
        objFunctionMap.put("request_count","1000");
        objFunctionMap.put("request_max","400");

        return objFunctionMap;
    }

}

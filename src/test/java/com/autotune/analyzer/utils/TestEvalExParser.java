/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeSupportedTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEvalExParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEvalExParser.class);
    Set<String> objFunctionsList = KruizeSupportedTypes.OBJECTIVE_FUNCTION_LIST;

    @Test
    public void testValidate() {

    for (String objFunction : objFunctionsList) {
        ArrayList<Metric> functionVariables = getFunctionVariables();
        assertEquals(true, new EvalExParser().validate(objFunction, functionVariables));
        }
    }

    private ArrayList<Metric> getFunctionVariables() {
        ArrayList<Metric> metricArrayList = new ArrayList<>();
        String sloJson = readDataFromJson();
        if (sloJson.equals("-1"))
            return metricArrayList;

        JSONObject functionVariablesObject = new JSONObject(sloJson);

        JSONArray functionVariables = functionVariablesObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);

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

    private String readDataFromJson() {
        String fileName = "function_variables.json";
        InputStream ioStream;
        try {
            ioStream = TestEvalExParser.class.getClassLoader().getResourceAsStream(fileName);
        } catch (NullPointerException nullPointerException){
            LOGGER.error("Invalid FileName or File is missing ");
            return "-1";
        }
        InputStreamReader isReader = new InputStreamReader(ioStream);
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str = "";
        try {
            while((str = reader.readLine())!= null){
                sb.append(str);
            }
        } catch (IOException ioException) {
            LOGGER.error("File Read Error");
        }
        return sb.toString();
    }

    @Test
    public void testParse() {

        for(String objFunction : objFunctionsList) {
            Map<String, String> objFunctionMap = new HashMap<>();
            assertEquals(AnalyzerErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MAP_MISSING, new EvalExParser().parse(objFunction, objFunctionMap));
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

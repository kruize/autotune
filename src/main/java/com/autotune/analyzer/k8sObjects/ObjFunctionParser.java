package com.autotune.analyzer.k8sObjects;

import com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class ObjFunctionParser {

    public static String testParser(String objFuncData, String valueType, Map<String, String> objFuncMap) {

        BigDecimal result = null;

        // Validate the objFuncData, valueType, objFuncMap
        String validationCheck = validate(objFuncData, valueType, objFuncMap);

        if(!validationCheck.equalsIgnoreCase("True"))
            return validationCheck;

        //Evaluate the result if validation passes
        // TODO: Below code is Work in Progress
        result = new Expression(objFuncData)
                .with("request_sum", objFuncMap.get("request_sum"))
                .and("request_count", objFuncMap.get("request_sum"))
                .setPrecision(128)
                .setRoundingMode(RoundingMode.UP)
                .eval();
        return result.toString();
    }

    private static String validate(String objFuncData, String valueType, Map<String, String> objFuncMap) {

        return "True";
    }

    public static void main(String[] args) {

        String objFuncData = "request_sum/request_count";
        String valueType = "String";
        Map<String, String> objFuncMap = new HashMap<>();
        objFuncMap.put("request_sum", "1");
        objFuncMap.put("request_count", "3");
        System.out.println(testParser(objFuncData, valueType, objFuncMap));    }
}

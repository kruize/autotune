package com.autotune.analyzer.k8sObjects;

import com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class ObjFunctionParser {

    public static String testParser(String objFuncData, String valueType, Map<String, String> objFuncMap) {

        BigDecimal result = null;

        // Validate the objFuncData, valueType, objFuncMap
        String validationCheck = validate(objFuncData, valueType, objFuncMap);

        if(!validationCheck.equalsIgnoreCase("True"))
            return validationCheck;

        //Evaluate the result if validation passes
        result = new Expression(objFuncData)
                .with("request_sum", objFuncMap.get("request_sum"))
                .and("request_count", objFuncMap.get("request_sum"))
                .setPrecision(128)
                .setRoundingMode(RoundingMode.UP)
                .eval();
        return result.toString();
    }

    private static String validate(String objFuncData, String valueType, Map<String, String> objFuncMap) {



        return objFuncData;
    }

}

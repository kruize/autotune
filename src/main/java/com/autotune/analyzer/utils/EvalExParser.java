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
import com.udojava.evalex.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * We're using the EvalEx - Java Expression Evaluator for the objective function evaluation
 * EvalEx GitHub URL : https://github.com/uklimaschewski/EvalEx
 * Copyright 2012-2021 by Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 * To validate and parse the objective function by using EvaluatorEx library
 */
public class EvalExParser implements AlgebraicParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvalExParser.class);

    /**
     *  parse the objective function and return the result based on the valueType received
     * @param objFunction
     * @param valueType
     * @param objFunctionMap
     * @return
     */
    @Override
    public String parse(String objFunction, String valueType, Map<String, String> objFunctionMap) {

        BigDecimal result;

        if(objFunctionMap.isEmpty())
            return AnalyzerErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MAP_MISSING;

        if( objFunction.isBlank() || objFunction == null)
            return AnalyzerErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MISSING;

        List<String> objFunctionMapKeys = new ArrayList<>(objFunctionMap.keySet());
        Expression expressionEvaluator = new Expression(objFunction);

        for(String key : objFunctionMapKeys){
            expressionEvaluator = expressionEvaluator.and(key, objFunctionMap.get(key));
        }
        result = expressionEvaluator.setPrecision(3)
                .setRoundingMode(RoundingMode.UP)
                .eval();

        return result.toString();
    }

    /**
     * validate the objective function and return boolean based on the result
     * @param objFunction
     * @param functionVariables
     * @return
     */
    @Override
    public Boolean validate(String objFunction, ArrayList<Metric> functionVariables) {

        List<String> variableNames = new ArrayList<>();
        for(Metric functionVariable : functionVariables){
            variableNames.add(functionVariable.getName());
        }

        LOGGER.info("Variables: {}", variableNames);

        /*
            Remove all the variables from the objFunction matching with variables present in variableNames List
            so that the function is left with only brackets and operators
         */
        String objFunctionOperators = "";
        for(String variable : variableNames) {
            objFunctionOperators = objFunction.replaceAll("[a-zA-Z _]","");

        }
        LOGGER.info("Objective Func Operators: {}", objFunctionOperators);

        //Remove extra whitespaces from the string
        objFunctionOperators = objFunctionOperators.replaceAll("\\s", "");

        //check if there's any unnecessary character apart from the mathematical operators
        if (!containsValidMathOperators(objFunctionOperators)) {
            return false;
        }

        // check if brackets in objective function string are balanced
        return areBracketsBalanced(objFunctionOperators);
    }

    /**
     * check if the objFunction received contains valid mathematical operators
     * @param objFunctionOperators
     * @return
     */
    private boolean containsValidMathOperators(String objFunctionOperators) {

        Deque<Character> mathOperators = new ArrayDeque<>();
        for (int i = 0; i < objFunctionOperators.length(); i++) {

            char charInObjFunction = objFunctionOperators.charAt(i);
            if(!(charInObjFunction == ')' || charInObjFunction == '(' || isNumeric(String.valueOf(charInObjFunction)) || charInObjFunction == '.')) {
                mathOperators.push(charInObjFunction);
            }
        }
        for (Character mathOperator : mathOperators) {

            if(!AutotuneSupportedTypes.MATH_OPERATORS_SUPPORTED.contains(String.valueOf(mathOperator))) {
                return false;
            }
        }
        return true;
    }

    /**
     * check if the string contains only numbers and return boolean result
     * @param strNum
     * @return
     */
    private static boolean isNumeric(String strNum) {

        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * check if the brackets present in the objective function are balanced
     * @param objFunction
     * @return
     */
    private boolean areBracketsBalanced(String objFunction) {

        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < objFunction.length(); i++)  {
            char bracket = objFunction.charAt(i);
            if(bracket == ')') {
                if(stack.isEmpty())
                    return false;
                Character stackTop = stack.pop();
                if( stackTop != '(')
                    return false;
            } else {
                if(bracket == '(') {
                    stack.push(bracket);
                }
            }
        }
        // Check Empty Stack
        return (stack.isEmpty());
    }
}

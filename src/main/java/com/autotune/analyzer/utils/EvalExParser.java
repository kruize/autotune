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
 * We're using the EvalEx - Java Expression Evaluator for the objective function evaluation
 * EvalEx GitHub URL : https://github.com/uklimaschewski/EvalEx
 * Copyright 2012-2021 by Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *******************************************************************************/

// TODO: Add javadoc for Evaluator library
package com.autotune.analyzer.utils;

import com.autotune.analyzer.k8sObjects.Metric;
import com.udojava.evalex.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class EvalExParser implements AlgebraicParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvalExParser.class);
    @Override
    public String parse(String objFunction, String valueType, Map<String, String> objFunctionMap) {

        BigDecimal result;

        List<String> objFunctionMapKeys = new ArrayList<>(objFunctionMap.keySet());
        Expression temp = new Expression(objFunction);

        for(String key : objFunctionMapKeys){
            temp = temp.and(key, objFunctionMap.get(key));
        }
        result = temp.setPrecision(3)
                .setRoundingMode(RoundingMode.UP)
                .eval();

        return result.toString();
    }

    @Override
    public Boolean validate(String objFunction, ArrayList<Metric> functionVariables) {

        List<String> variables = new ArrayList<>();
        for(Metric functionVariable : functionVariables){
            variables.add(functionVariable.getName());
        }
        System.out.println("variables: "+variables);
        for(String variable : variables){
            objFunction = objFunction.replaceAll(variable,"");
        }
        LOGGER.info("Objective Func: ", objFunction);

        //Remove extra whitespaces from the string
        objFunction = objFunction.replaceAll("\\s", "");

        //check if there's any unnecessary character apart from the mathematical operators
        if(!containsValidMathOperators(objFunction)){
            return false;
        }

        // check if brackets in objective function string are balanced
        if(!areBracketsBalanced(objFunction)){
            return false;
        }

        return true;
    }

    private boolean containsValidMathOperators(String objFunction) {

        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < objFunction.length(); i++)  {
            char character = objFunction.charAt(i);
            if(!(character == ')' || character == '(' || isNumeric(String.valueOf(character)))) {
                stack.push(character);
            }
        }
        for(Character ch : stack){
            if(!AutotuneSupportedTypes.MATH_OPERATORS_SUPPORTED.contains(String.valueOf(ch))){
                return false;
            }
        }
        return true;
    }

    //check if string is numeric
    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // function to check if brackets are balanced

    private boolean areBracketsBalanced(String objFunction) {

        Stack<Character> stack = new Stack<>();

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

//    public static void main(String[] args) {
//
//        List<String> variables = new ArrayList<>(List.of("transaction_response_time" , "throughput", "max_response_time"));
//        String objFunction = "(( throughput / transaction_response_time) /  max_response_time) * 100";
//        System.out.println(new EvalExParser().validate(objFunction, variables));
//    }
}

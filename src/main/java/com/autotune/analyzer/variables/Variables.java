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
 *******************************************************************************/
package com.autotune.analyzer.variables;

import com.autotune.analyzer.utils.DAConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Update queries with AutotuneQueryVariables
 */
public class Variables
{
    /**
     * For a query, update the variables with the values from AutotuneQueryVariable object
     * @param application
     * @param namespace
     * @param query
     * @param variablesArray
     * @return
     * @throws IOException
     */
    public static String updateQueryWithVariables(String application, String namespace, String query, ArrayList<Map<String, String>> variablesArray) throws IOException {
        if (query == null)
            return null;

        if (variablesArray != null) {
            for (Map<String, String> variable : variablesArray) {
                String key = variable.get("name");
                String value = variable.get("value");
                query = query.replace(key, value);

            }
        }

        query = replaceGlobalVariablesForQuery(application, namespace, query);
        return query;
    }

    /**
     * Replace global variables for a query
     * @param application
     * @param namespace
     * @param query
     * @return
     */
    private static String replaceGlobalVariablesForQuery(String application, String namespace, String query) {
        if (application != null) {
            query = query.replace(DAConstants.POD_VARIABLE, application);
        }
        if (namespace != null) {
            query = query.replace(DAConstants.NAMESPACE_VARIABLE, namespace);
        }
        return query;
    }
}

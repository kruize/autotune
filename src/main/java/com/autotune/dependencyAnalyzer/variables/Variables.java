package com.autotune.dependencyAnalyzer.variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class Variables
{
    public static String updateQueryWithVariables(String application, String namespace, String query, ArrayList<Map<String, String>> variablesArray) throws IOException
    {
        if (query == null)
            return null;

        if (variablesArray != null)
        {
            for (Map<String, String> variable : variablesArray)
            {
                String key = variable.get("name");
                String value = variable.get("value");
                query = query.replace(key, value);

            }
        }

        query = replaceGlobalVariablesForQuery(application, namespace, query);
        return query;
    }

    private static String replaceGlobalVariablesForQuery(String application, String namespace, String query)
    {
        final String POD = "$POD$";
        final String NAMESPACE = "$NAMESPACE$";

        if (application != null)
        {
            query = query.replace(POD, application);
        }
        if (namespace != null)
        {
            query = query.replace(NAMESPACE, namespace);
        }
        return query;
    }
}

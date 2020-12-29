package com.autotune.dependencyAnalyzer.datasource;

import java.net.MalformedURLException;
import java.util.List;

public interface DataSource
{
    /**
     * Run the getAllAppsQuery and return the list of applications matching the layer.
     * @param query GetAllApps query for the layer
     * @param key The key to search for in the response
     * @return List of all applications from the query
     * @throws MalformedURLException
     */
    List<String> getAppsForLayer(String query, String key) throws MalformedURLException;
}

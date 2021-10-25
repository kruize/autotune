package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentMetrics implements ConvertToJSON {
    private String name;
    private String query;
    private String datasource;
    private EMConfigDeploymentMetricResults results;

    public EMConfigDeploymentMetrics() {
        this.results = new EMConfigDeploymentMetricResults();
    }

    public EMConfigDeploymentMetrics(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if(!jsonObject.has(EMConstants.EMJSONKeys.NAME)
            || !jsonObject.has(EMConstants.EMJSONKeys.QUERY)) {
            throw new IncompatibleInputJSONException();
        }
        this.name = jsonObject.getString(EMConstants.EMJSONKeys.NAME);
        this.query = jsonObject.getString(EMConstants.EMJSONKeys.QUERY);
        this.datasource = jsonObject.getString(EMConstants.EMJSONKeys.DATASOURCE);
        this.results = new EMConfigDeploymentMetricResults();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public EMConfigDeploymentMetricResults getResults() {
        return results;
    }

    public void setResults(EMConfigDeploymentMetricResults results) {
        this.results = results;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject =  new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.NAME, getName());
        jsonObject.put(EMConstants.EMJSONKeys.QUERY, getQuery());
        jsonObject.put(EMConstants.EMJSONKeys.DATASOURCE, getDatasource());
        if (getResults().isResultsAvailable()) {
            jsonObject.put(EMConstants.EMJSONKeys.METRICS_RESULTS, getResults().toJSON());
        }
        return jsonObject;
    }
}

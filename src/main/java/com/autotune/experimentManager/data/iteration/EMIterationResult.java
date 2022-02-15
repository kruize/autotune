package com.autotune.experimentManager.data.iteration;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.util.EMDataOperator;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMIterationResult {
    private ArrayList<EMMetricData> warmUpResults;
    private ArrayList<EMMetricData> measurementResults;
    private ArrayList<EMMetricData> summarisedResults;
    private boolean isLatest;

    public EMIterationResult() {
        this.warmUpResults = new ArrayList<EMMetricData>();
        this.measurementResults = new ArrayList<EMMetricData>();
        this.summarisedResults = new ArrayList<EMMetricData>();
        this.isLatest = true;
    }

    public ArrayList<EMMetricData> getWarmUpResults() {
        return warmUpResults;
    }

    public ArrayList<EMMetricData> getMeasurementResults() {
        return measurementResults;
    }

    public ArrayList<EMMetricData> getSummarisedResults() {
        if(!isLatest) {
            this.summarisedResults = EMDataOperator.calculateSummaryResults(getWarmUpResults(), getMeasurementResults());
        }
        return summarisedResults;
    }

    public void addToWarmUpList(EMMetricData emMetricData) {
        getWarmUpResults().add(emMetricData);
        isLatest = false;
    }

    public void addToMeasurementList(EMMetricData emMetricData) {
        getMeasurementResults().add(emMetricData);
        isLatest = false;
    }
}

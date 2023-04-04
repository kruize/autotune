package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;

import java.util.Map;

public interface ExperimentDAO {

    //Add New experiments from local storage to DB and set status to Inprogress
    public ValidationOutputData addExperimentToDB(
            KruizeObject kruizeObject
    );

    //Update experiment status
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status);

    //Add experiment results from local storage to DB and set status to Inprogress
    public ValidationOutputData addResultsToDB(
            ExperimentResultData resultData
    );

    //If Kruize object restarts load all experiment which are in inprogress
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap);

    //Delete experiment
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName);
}

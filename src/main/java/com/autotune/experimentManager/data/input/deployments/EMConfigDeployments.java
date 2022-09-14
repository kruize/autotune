package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigDeployments implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigDeployments.class);
    private EMConfigTrainingDeployment trainingDeployment = null;
    private EMConfigProductionDeployment productionDeployment = null;

    public EMConfigTrainingDeployment getTrainingDeployment() {
        return trainingDeployment;
    }

    public void setTrainingDeployment(EMConfigTrainingDeployment trainingDeployment) {
        this.trainingDeployment = trainingDeployment;
    }

    public EMConfigProductionDeployment getProductionDeployment() {
        return productionDeployment;
    }

    public void setProductionDeployment(EMConfigProductionDeployment productionDeployment) {
        this.productionDeployment = productionDeployment;
    }

    public EMConfigDeployments(JSONObject jsonObject) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        LOGGER.info("Creating EMConfigDeployment");
        if (!jsonObject.has(AutotuneConstants.JSONKeys.DEPLOYMENTS)){
            throw new IncompatibleInputJSONException();
        }
        JSONArray deploymentsArray = jsonObject.getJSONArray(AutotuneConstants.JSONKeys.DEPLOYMENTS);
        for (Object o : deploymentsArray) {
            JSONObject job = (JSONObject) o;
            if (!job.has(AutotuneConstants.JSONKeys.TYPE)) {
                throw new IncompatibleInputJSONException();
            }
            if (job.getString(AutotuneConstants.JSONKeys.TYPE).equalsIgnoreCase(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING)) {
                this.trainingDeployment = new EMConfigTrainingDeployment(job);
            } else if (job.getString(AutotuneConstants.JSONKeys.TYPE).equalsIgnoreCase(EMConstants.EMConfigDeployments.DeploymentTypes.PRODUCTION)) {
                this.productionDeployment = new EMConfigProductionDeployment(job);
            }
        }
    }

    public EMConfigDeployments() {

    }

    @Override
    public JSONObject toJSON() {
        JSONArray jsonArray = new JSONArray();
        if (null != getTrainingDeployment())
            jsonArray.put(getTrainingDeployment().toJSON());
        if (null != getProductionDeployment())
            jsonArray.put(getProductionDeployment().toJSON());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.DEPLOYMENTS, jsonArray);
        return jsonObject;
    }
}

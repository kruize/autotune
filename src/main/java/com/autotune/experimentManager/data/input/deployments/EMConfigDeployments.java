package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

public class EMConfigDeployments extends DataEditor<EMConfigDeployments> implements ConvertToJSON {

    private EMConfigTrainingDeployment trainingDeployment;
    private EMConfigProductionDeployment productionDeployment;

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

    public EMConfigDeployments(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENTS)){
            throw new IncompatibleInputJSONException();
        }
        JSONArray deploymentsArray = jsonObject.getJSONArray(EMConstants.EMJSONKeys.DEPLOYMENTS);
        for (Object o : deploymentsArray) {
            JSONObject job = (JSONObject) o;
            if (!job.has(EMConstants.EMJSONKeys.TYPE)) {
                throw new IncompatibleInputJSONException();
            }
            if (job.getString(EMConstants.EMJSONKeys.TYPE).equalsIgnoreCase(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING)) {
                this.trainingDeployment = new EMConfigTrainingDeployment(job);
            } else if (job.getString(EMConstants.EMJSONKeys.TYPE).equalsIgnoreCase(EMConstants.EMConfigDeployments.DeploymentTypes.PRODUCTION)) {
                this.productionDeployment = new EMConfigProductionDeployment(job);
            }
        }
    }

    public EMConfigDeployments() {

    }

    @Override
    public EMConfigDeployments edit() {
        return this;
    }

    @Override
    public EMConfigDeployments done() {
        return this;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(getTrainingDeployment().toJSON());
        jsonArray.put(getProductionDeployment().toJSON());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.DEPLOYMENTS, jsonArray);
        return jsonObject;
    }
}

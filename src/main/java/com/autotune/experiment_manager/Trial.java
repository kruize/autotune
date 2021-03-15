package com.autotune.experiment_manager;

import org.json.JSONArray;

import java.util.ArrayList;

public class Trial
{
	String id;
	int trialNumber;
	int trialRun;
	String deploymentName;

	ArrayList<Config> updateConfig = new ArrayList<>();
	JSONArray queries = new JSONArray();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getTrialNumber() {
		return trialNumber;
	}

	public void setTrialNumber(int trialNumber) {
		this.trialNumber = trialNumber;
	}

	public int getTrialRun() {
		return trialRun;
	}

	public void setTrialRun(int trialRun) {
		this.trialRun = trialRun;
	}

	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public ArrayList<Config> getUpdateConfig() {
		return updateConfig;
	}

	public void setUpdateConfig(ArrayList<Config> updateConfig) {
		this.updateConfig = updateConfig;
	}

	public JSONArray getQueries() {
		return queries;
	}

	public void setQueries(JSONArray queries) {
		this.queries = queries;
	}
}

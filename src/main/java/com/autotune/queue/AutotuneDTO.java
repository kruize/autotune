package com.autotune.queue;

import java.io.Serializable;

import org.json.JSONObject;

import com.autotune.em.utils.EMUtils.EMProcessorType;


public class AutotuneDTO implements Serializable {
	private static final long serialVersionUID = 8789442223058487193L;
	private int id;
	private String name;
	private JSONObject inputObject;
	private StringBuffer infoMessage;
	private StringBuffer errorMessage;
	private EMProcessorType processorName;
	
	public AutotuneDTO() {}
	
	public AutotuneDTO(int experimentId, String componentName, EMProcessorType processorName, JSONObject inputObject) {
		this.id = experimentId;
		this.name = componentName;
		this.processorName = processorName;
		this.inputObject = inputObject;
		this.infoMessage = new StringBuffer();
		this.errorMessage = new StringBuffer();
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public JSONObject getInputObject() {
		return inputObject;
	}

	public void setInputObject(JSONObject inputObject) {
		this.inputObject = inputObject;
	}

	public EMProcessorType getProcessorName() {
		return processorName;
	}

	public void setProcessorName(EMProcessorType processorName) {
		this.processorName = processorName;
	}
	
	public StringBuffer getInfoMessage() {
		return infoMessage;
	}

	public void setInfoMessage(StringBuffer infoMessage) {
		this.infoMessage = infoMessage;
	}

	public StringBuffer getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(StringBuffer errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return "id=" + id + ", name=" + name + ", processorName=" + processorName + ", inputJSON= " + inputObject;
	}
}

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
package com.autotune.queue;

import java.io.Serializable;

import com.autotune.em.utils.EMUtils.EMProcessorType;


public class AutotuneDTO implements Serializable {
	private static final long serialVersionUID = 8789442223058487193L;
	private int id;
	private String name;
	private String url;
	private StringBuffer infoMessage;
	private StringBuffer errorMessage;
	private EMProcessorType processorName;
	
	public AutotuneDTO() {}
	
	public AutotuneDTO(int experimentId, String componentName, String dataURL,  EMProcessorType processorName) {
		this.id = experimentId;
		this.name = componentName;
		this.url = dataURL;
		this.processorName = processorName;
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
		return "AutotuneDTO{" +
				"id=" + id +
				", name='" + name + '\'' +
				", url='" + url + '\'' +
				", infoMessage=" + infoMessage +
				", errorMessage=" + errorMessage +
				", processorName=" + processorName +
				'}';
	}
}

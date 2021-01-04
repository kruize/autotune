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
package com.autotune.dependencyAnalyzer.k8sObjects;

/**
 * Holds information about the selector key in the autotune object yaml
 *
 * Example:
 *   selector:
 *     matchLabel: "org.kubernetes.io/name"
 *     matchLabelValue: "spring-petclinic"
 *     matchRoute: ""
 *     matchURI: ""
 *     matchService: "https"
 */
public class SelectorInfo
{
	private String matchLabel;
	private String matchValue;
	private String matchURI;
	private String matchRoute;
	private String matchService;

	public String getMatchLabel() {
		return matchLabel;
	}

	public void setMatchLabel(String matchLabel) {
		this.matchLabel = matchLabel;
	}

	public String getMatchValue() {
		return matchValue;
	}

	public void setMatchValue(String matchValue) {
		this.matchValue = matchValue;
	}

	public String getMatchURI() {
		return matchURI;
	}

	public void setMatchURI(String matchURI) {
		this.matchURI = matchURI;
	}

	public String getMatchRoute() {
		return matchRoute;
	}

	public void setMatchRoute(String matchRoute) {
		this.matchRoute = matchRoute;
	}

	public String getMatchService() {
		return matchService;
	}

	public void setMatchService(String matchService) {
		this.matchService = matchService;
	}

	@Override
	public String toString() {
		return "SelectorInfo{" +
				"matchLabel='" + matchLabel + '\'' +
				", matchValue='" + matchValue + '\'' +
				", matchURI='" + matchURI + '\'' +
				", matchRoute='" + matchRoute + '\'' +
				", matchService='" + matchService + '\'' +
				'}';
	}
}

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
package com.autotune.analyzer.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;

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
public final class SelectorInfo
{
	private final String matchLabel;
	private final String matchLabelValue;
	private final String matchURI;
	private final String matchRoute;
	private final String matchService;

	public SelectorInfo(String matchLabel,
			String matchLabelValue,
			String matchURI,
			String matchRoute,
			String matchService) throws InvalidValueException {
		//TODO Can this be blank?
		if (matchLabel != null && !matchLabel.equals(""))
			this.matchLabel = matchLabel;
		else throw new InvalidValueException("Invalid MatchLabel");

		if (matchLabelValue != null && !matchLabelValue.equals(""))
			this.matchLabelValue = matchLabelValue;
		else throw new InvalidValueException("Invalid MatchLabelValue");

		this.matchURI = matchURI;
		this.matchRoute = matchRoute;
		this.matchService = matchService;
	}

	public SelectorInfo(SelectorInfo copy) {
		this.matchLabel = copy.getMatchLabel();
		this.matchLabelValue = copy.getMatchLabelValue();
		this.matchURI = copy.getMatchURI();
		this.matchRoute = copy.getMatchRoute();
		this.matchService = copy.getMatchService();
	}

	public String getMatchLabel() {
		return matchLabel;
	}

	public String getMatchLabelValue() {
		return matchLabelValue;
	}

	public String getMatchURI() {
		return matchURI;
	}

	public String getMatchRoute() {
		return matchRoute;
	}

	public String getMatchService() {
		return matchService;
	}

	@Override
	public String toString() {
		return "SelectorInfo{" +
				"matchLabel='" + matchLabel + '\'' +
				", matchLabelValue='" + matchLabelValue + '\'' +
				", matchURI='" + matchURI + '\'' +
				", matchRoute='" + matchRoute + '\'' +
				", matchService='" + matchService + '\'' +
				'}';
	}
}

/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.updater.vpa;

import com.autotune.analyzer.recommendations.updater.RecommendationUpdaterImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ApiextensionsAPIGroupDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VpaUpdaterImpl extends RecommendationUpdaterImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(VpaUpdaterImpl.class);
    private static VpaUpdaterImpl vpaUpdater = new VpaUpdaterImpl();

    private KubernetesClient kubernetesClient;
    private ApiextensionsAPIGroupDSL apiextensionsClient;


    private VpaUpdaterImpl() {
        this.kubernetesClient = new DefaultKubernetesClient();
        this.apiextensionsClient = kubernetesClient.apiextensions();
    }

    public static VpaUpdaterImpl getInstance() {
        if (vpaUpdater == null) {
            vpaUpdater = new VpaUpdaterImpl();
        }
        return vpaUpdater;
    }

    /**
     * Checks whether the necessary updater dependencies are installed or available in the system.
     * @return boolean true if the required updaters are installed, false otherwise.
     */
    @Override
    public boolean isUpdaterInstalled() {
        LOGGER.info(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CHECKING_IF_UPDATER_INSTALLED,
                AnalyzerConstants.RecommendationUpdaterConstants.SupportedUpdaters.VPA);
        // checking if VPA CRD is present or not
        CustomResourceDefinitionList crdList = apiextensionsClient.v1().customResourceDefinitions().list();
        boolean isVpaInstalled = crdList.getItems().stream().anyMatch(crd -> AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_PLURAL.equalsIgnoreCase(crd.getSpec().getNames().getKind()));
        if (isVpaInstalled) {
            LOGGER.info(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.FOUND_UPDATER_INSTALLED, AnalyzerConstants.RecommendationUpdaterConstants.SupportedUpdaters.VPA);
        } else {
            LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.UPDATER_NOT_INSTALLED, AnalyzerConstants.RecommendationUpdaterConstants.SupportedUpdaters.VPA);
        }
        return isVpaInstalled;
    }
}

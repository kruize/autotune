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

package com.autotune.analyzer.autoscaler;

import com.autotune.analyzer.autoscaler.validator.AutoscalerGuard;
import com.autotune.analyzer.autoscaler.validator.ResourceValidator;
import com.autotune.analyzer.autoscaler.vpa.VpaAutoscalerImpl;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.autoscaler.accelerator.AcceleratorAutoscalerImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.service.ExperimentDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutoscalerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscalerService.class);

    public static void initiateAutoscalerService() {
        try {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            LOGGER.info(AnalyzerConstants.AutoscalerConstants.InfoMsgs.STARTING_SERVICE);
            executorService.scheduleAtFixedRate(() -> {
                try {
                    AutoscalerImpl autoscaler = new AutoscalerImpl();
                    Map<String, KruizeObject> experiments = getAutoModeExperiments();
                    for (Map.Entry<String, KruizeObject> experiment : experiments.entrySet()) {
                        // autoscaler guard checks
                        ValidationOutputData validationOutputData = AutoscalerGuard.checkAutoscalerGuard(experiment.getValue());
                        if (validationOutputData.isSuccess()) {
                            KruizeObject kruizeObject = autoscaler.generateResourceRecommendationsForExperiment(experiment.getValue().getExperimentName());

                            // TODO:// add default updater in kruizeObject and check if GPU recommendations are present
                            if (kruizeObject.getDefaultUpdater() == null) {
                                kruizeObject.setDefaultUpdater(AnalyzerConstants.AutoscalerConstants.SupportedUpdaters.VPA);
                            }

                            if (kruizeObject.getDefaultUpdater().equalsIgnoreCase(AnalyzerConstants.AutoscalerConstants.SupportedUpdaters.VPA)) {
                                VpaAutoscalerImpl vpaUpdater = VpaAutoscalerImpl.getInstance();
                                vpaUpdater.applyResourceRecommendationsForExperiment(kruizeObject);
                            }
                        } else {
                            LOGGER.error(validationOutputData.getMessage());
                        }

                        if (kruizeObject.getDefaultUpdater().equalsIgnoreCase(AnalyzerConstants.AutoscalerConstants.SupportedUpdaters.ACCELERATOR)) {
                            AcceleratorAutoscalerImpl acceleratorUpdater = AcceleratorAutoscalerImpl.getInstance();
                            acceleratorUpdater.applyResourceRecommendationsForExperiment(kruizeObject);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }, AnalyzerConstants.AutoscalerConstants.DEFAULT_INITIAL_DELAY,
                    AnalyzerConstants.AutoscalerConstants.DEFAULT_SLEEP_INTERVAL,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.AutoscalerErrors.UPDATER_SERVICE_START_ERROR + e.getMessage());
        }
    }

    private static Map<String, KruizeObject> getAutoModeExperiments() {
        try {
            LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.CHECKING_AUTO_EXP);
            Map<String, KruizeObject> mainKruizeExperimentMap = new ConcurrentHashMap<>();
            new ExperimentDBService().loadAllLMExperiments(mainKruizeExperimentMap);
            // filter map to only include entries where mode is auto or recreate
            Map<String, KruizeObject> filteredMap = mainKruizeExperimentMap.entrySet().stream()
                    .filter(entry -> {
                        String mode = entry.getValue().getMode();
                        return AnalyzerConstants.AUTO.equalsIgnoreCase(mode) || AnalyzerConstants.RECREATE.equalsIgnoreCase(mode);
                    })
                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
            return filteredMap;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new HashMap<>();
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.utils;

import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.EMMetricInput;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class EMUtil {
    /**
    * EMExpStages is a collection of all stages that an experiment trial goes through to complete its lifecycle.
    *
    * Each trial starts at INIT stage and ends at EXIT stage
    *
    * Each subsequent stage between INIT and EXIT represent a particular portion of trial lifecycle as described below
    *
    * CREATE_CONFIG:
    *   Description: Stage in EM Trial Lifecycle where the configuration for a particular trial is being created
    *   Predecessor Stage: INIT
    *   Successor Stage: DEPLOY_CONFIG
    *   Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
    *
    * DEPLOY_CONFIG:
    *   Description: Stage in EM Trial Lifecycle where the created configuration for a trial is deployed in the node
    *   Predecessor Stage: CREATE_CONFIG
    *   Successor Stage: INITIATE_TRAIL_RUN_PHASE
    *   Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
    *
    * Details of respective stages will be added once they are implemented
    *
    */
    public enum EMExpStages {
        /**
         * Initial stage
         */
        INIT(0, 1, false, null),
        /**
         * CREATE_CONFIG:
         *   Description: Stage in EM Trial Lifecycle where the configuration for a particular trial is being created
         *   Predecessor Stage: INIT
         *   Successor Stage: DEPLOY_CONFIG
         *   Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
         */
        CREATE_CONFIG(1, 1, false, EMConstants.TransitionClasses.CREATE_CONFIG),
        /**
         * DEPLOY_CONFIG:
         *   Description: Stage in EM Trial Lifecycle where the created configuration for a trial is deployed in the node
         *   Predecessor Stage: CREATE_CONFIG
         *   Successor Stage: INITIATE_TRAIL_RUN_PHASE
         *   Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
         */
        DEPLOY_CONFIG(2, 1, false, EMConstants.TransitionClasses.DEPLOY_CONFIG),
        INITIATE_TRIAL_RUN_PHASE(3, 1, false, EMConstants.TransitionClasses.INITIATE_TRIAL_RUN_PHASE),
        INITIAL_LOAD_CHECK(3, 2, true, EMConstants.TransitionClasses.INITIAL_LOAD_CHECK),
        LOAD_CONSISTENCY_CHECK(3, 3, true, EMConstants.TransitionClasses.LOAD_CONSISTENCY_CHECK),
        INITIATE_METRICS_COLLECTION_PHASE(4, 1, false, EMConstants.TransitionClasses.INITIATE_METRICS_COLLECTION_PHASE),
        COLLECT_METRICS(4, 1, true, EMConstants.TransitionClasses.COLLECT_METRICS),
        CREATE_RESULT_DATA(5, 1, false, EMConstants.TransitionClasses.CREATE_RESULT_DATA),
        SEND_RESULT_DATA(5, 2, false, EMConstants.TransitionClasses.SEND_RESULT_DATA),
        CLEAN_OR_ROLLBACK_DEPLOYMENT(6, 1, true, EMConstants.TransitionClasses.CLEAN_OR_ROLLBACK_DEPLOYMENT),
        /**
         * Final or exiting stage
         */
        EXIT(7, 1, false, null)
        ;

        private int stage;
        private int intermediate_stage;
        private boolean isScheduled;
        private String className;
        private static final EMExpStages values[] = values();

        private EMExpStages(final int stage, final int intermediate_stage, final boolean isScheduled, final String className) {
            this.stage = stage;
            this.intermediate_stage = intermediate_stage;
            this.isScheduled = isScheduled;
            this.className = className;
        }

        /**
         * Returns stage id
         */
        public int getStage() {
            return stage;
        }

        /**
         * Returns intermediate stage id
         */
        public int getIntermediate_stage() {
            return intermediate_stage;
        }

        /**
         * Returns if the stage is a scheduled stage
         */
        public boolean isScheduled() {
            return isScheduled;
        }

        /**
         * Returns the name of the class which is responsible for processing a particular stage
         */
        public String getClassName() {
            return className;
        }

        public static EMExpStages get(int ordinal) { return values[ordinal]; }

        public static int getSize() {
            return values().length;
        }

    }

    /**
    * Statuses are the informative responses to the user at a high level about the life cycle of a trial
    *
    * If a trial is just created the status would be `CREATED`
    * If a trial is waiting for load or any other param to start or continue the status would be `WAIT`
    * If a trial is undergoing the metric collection or getting run to collect metrics the status would be `IN_PROGRESS`
    * If a trial has completed its iterations and sent metrics to USER and exited the status would be `COMPLETED`
    */
    public enum EMExpStatus {
        CREATED,
        WAIT,
        IN_PROGRESS,
        WAITING_FOR_LOAD,
        APPLYING_LOAD,
        COLLECTING_METRICS,
        COMPLETED
    }

    /**
     * Utility to return a Unique ID
     */
    public static String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the NSD (NameSpace and Deployment) key by concating two strings
     * `namespace` and `deploymentName` with a colon `:`
     */
    public static String formatNSDKey(String namespace, String deploymentName) {
        return (new StringBuilder())
                .append(namespace)
                .append(":")
                .append(deploymentName)
                .toString();
    }

    public JSONObject generateMetricsMap(ExperimentTrialData etd) {
        ArrayList<EMMetricInput> emMetricInputs = etd.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getMetrics();

        return null;
    }

    public enum InterceptorDetectionStatus {
        DETECTED,
        NOT_DETECTED
    }

    public enum InterceptorAvailabilityStatus {
        AVAILABLE,
        NOT_AVAILABLE
    }
}

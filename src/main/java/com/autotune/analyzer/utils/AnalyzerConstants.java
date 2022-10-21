/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.utils;

/**
 * Constants used accross Analyser Modules
 */
public class AnalyzerConstants {

    /**
     * Main status of experiments created for monitoring.
     */
    public enum AnalyserExpStatus {
        NEW,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    /**
     * These settings decide if experiments and updated results need to store
     * at DB or in Local session object.
     */
    public static class RunConfig {
        public static final String STORAGE_MODE = "local";  //"db"
    }

    public static class AnalyserKeys {
        public static final String ANALYSER_EXPERIMENTS_STORAGE_KEY = "MainExperimentsMAP";

        private AnalyserKeys() {
        }
    }
}

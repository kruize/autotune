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

package com.autotune.utils;

/**
 * Constants for Autotune module
 */
public class AutotuneConstants {
    private AutotuneConstants() { }

    /**
     * Holds the constants of env vars and values to start Autotune in different Modes
     */
    public static class StartUpMode {
        private StartUpMode() { }
        public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
        public static final String EM_ONLY_MODE = "EM_ONLY";
    }

    public static class UISMConstants {
        private UISMConstants() { }
        public static class UISMJsonKeys {
            private UISMJsonKeys() { }
            public static final String UISM_VERSION = "uism_version";
            public static final String NAMESPACES = "namespaces";
            public static final String NAMESPACE = "namespace";
            public static final String DEPLOYMENTS = "deployments";
            public static final String DATA = "data";
            public static final String STATUS = "status";
        }

        public static class UISMDefaults {
            private UISMDefaults() { }
            public static final String UISM_VERSION = "v0.1";
        }

        public static class UISMStandard {
            private UISMStandard() { }
            public static final String SUCCESS_STATUS = "success";
            public static final String FAILURE_STATUS = "failure";
        }
    }

}

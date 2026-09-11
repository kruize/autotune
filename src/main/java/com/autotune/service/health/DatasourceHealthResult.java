/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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
package com.autotune.service.health;

/**
 * Immutable health summary of a single datasource.
 * Serialised directly to JSON by Gson in the /health response.
 */
public class DatasourceHealthResult {

    private final String name;
    private final String provider;
    private final String status;

    public DatasourceHealthResult(String name, String provider, String status) {
        this.name     = name;
        this.provider = provider;
        this.status   = status;
    }

    public String getName()     { return name; }
    public String getProvider() { return provider; }
    public String getStatus()   { return status; }
}

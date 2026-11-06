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

import java.util.Date;

/**
 * Immutable result of a single datasource reachability/auth probe.
 * Serialised directly to JSON by Gson in the /health response.
 * The {@code message} field must never contain credentials, tokens or
 * connection strings — only safe, user-facing text from
 * {@link com.autotune.utils.KruizeConstants.HealthConstants.Messages}.
 */
public class DatasourceHealthResult {

    private final String name;
    private final String provider;
    private final String serviceName;
    private final String namespace;
    private final String url;
    private final String status;
    private final long   latencyMs;
    private final String message;
    private final Date   checkedAt;

    public DatasourceHealthResult(String name, String provider, String serviceName,
                                  String namespace, String url, String status,
                                  long latencyMs, String message, Date checkedAt) {
        this.name        = name;
        this.provider    = provider;
        this.serviceName = serviceName;
        this.namespace   = namespace;
        this.url         = url;
        this.status      = status;
        this.latencyMs   = latencyMs;
        this.message     = message;
        this.checkedAt   = checkedAt;
    }

    public String getName()        { return name; }
    public String getProvider()    { return provider; }
    public String getServiceName() { return serviceName; }
    public String getNamespace()   { return namespace; }
    public String getUrl()         { return url; }
    public String getStatus()      { return status; }
    public long   getLatencyMs()   { return latencyMs; }
    public String getMessage()     { return message; }
    public Date   getCheckedAt()   { return checkedAt; }
}

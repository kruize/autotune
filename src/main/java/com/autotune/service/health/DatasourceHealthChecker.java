/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Checks whether a single datasource is reachable and healthy.
 *
 * <p>Reuses {@link DataSourceOperatorImpl#getOperator(String)} for provider
 * dispatch and the existing {@code isServiceable()} probe — no new connection
 * or authentication code is introduced here.
 *
 * <p>To support a new provider in the future, add a branch in
 * {@link #check(DataSourceInfo)} that calls the relevant operator, or
 * split this class into an interface + implementations at that point.
 */
public class DatasourceHealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceHealthChecker.class);

    /**
     * Probes the given datasource and returns a fully-populated result.
     * Never throws — all failures are captured in the returned object.
     */
    public DatasourceHealthResult check(DataSourceInfo ds) {
        String name        = ds.getName();
        String provider    = ds.getProvider();
        String serviceName = ds.getServiceName();
        String namespace   = ds.getNamespace();
        String url         = ds.getUrl() != null ? ds.getUrl().toString() : "";

        long start = System.currentTimeMillis();
        try {
            DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(provider);
            if (op == null) {
                LOGGER.warn("No operator available for datasource provider: {}", provider);
                return down(name, provider, serviceName, namespace, url, start,
                        KruizeConstants.HealthConstants.Messages.NO_CHECKER_AVAILABLE);
            }

            CommonUtils.DatasourceReachabilityStatus status = op.isServiceable(ds);
            long latencyMs = System.currentTimeMillis() - start;

            if (status == CommonUtils.DatasourceReachabilityStatus.REACHABLE) {
                return new DatasourceHealthResult(name, provider, serviceName, namespace, url,
                        KruizeConstants.HealthConstants.ComponentStatus.UP, latencyMs,
                        KruizeConstants.HealthConstants.Messages.CONNECTION_SUCCESSFUL, new Date());
            }
            LOGGER.warn("Datasource {} reported not reachable during health check", name);
            return down(name, provider, serviceName, namespace, url, start,
                    KruizeConstants.HealthConstants.Messages.CONNECTION_REFUSED);

        } catch (UnknownHostException e) {
            LOGGER.warn("Health check for datasource {} failed — unknown host", name);
            return down(name, provider, serviceName, namespace, url, start,
                    KruizeConstants.HealthConstants.Messages.UNKNOWN_HOST);

        } catch (SocketTimeoutException e) {
            LOGGER.warn("Health check for datasource {} timed out", name);
            return down(name, provider, serviceName, namespace, url, start,
                    KruizeConstants.HealthConstants.Messages.CONNECTION_TIMEOUT);

        } catch (Exception e) {
            // Catches auth errors (401/403 surface as IOException), SSL failures, etc.
            // Never expose the exception message — it may contain credentials or tokens.
            LOGGER.warn("Health check for datasource {} failed: {}", name, e.getMessage());
            return down(name, provider, serviceName, namespace, url, start,
                    KruizeConstants.HealthConstants.Messages.CHECK_FAILED);
        }
    }

    private DatasourceHealthResult down(String name, String provider, String serviceName,
                                        String namespace, String url, long startMs, String message) {
        return new DatasourceHealthResult(name, provider, serviceName, namespace, url,
                KruizeConstants.HealthConstants.ComponentStatus.DOWN,
                System.currentTimeMillis() - startMs, message, new Date());
    }
}

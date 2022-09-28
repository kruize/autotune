/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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

package com.autotune.common.utils;

/**
 * This Class holds the utilities needed by the classes in common package
 */
public class CommonUtils {

    /**
     * AutotuneDatasourceTypes is an ENUM which holds different types of
     * datasources supported by Autotune
     *
     * For now only Queryable and File based datasources are supported
     */
    public enum AutotuneDatasourceTypes {
        /**
         * If the datasource is queryable (datastore, database)
         */
        QUERYABLE,
        /**
         * If the datasource is file based (Cgroup files)
         */
        FILE,
    }

    /**
     * AddDataSourceStatus is an ENUM which holds the possible statuses
     * to return for adding a datasource to a collection in Autotune
     *
     * For now Success and Datasource not reachable are two possibilities
     * Failure status is parked as a placeholder for now to fit for an exact use case which might arise
     */
    public enum AddDataSourceStatus {
        SUCCESS,
        FAILURE,
        DATASOURCE_NOT_REACHABLE
    }

    /**
     * DatasourceReachabilityStatus is a ENUM which holds the possible statuses
     * to return for checking if a datasource is reachable
     *
     * REACHABLE - states that the datasource is reachable
     * NOT_REACHABLE - states that the datasource is not reachable
     */
    public enum DatasourceReachabilityStatus {
        REACHABLE,
        NOT_REACHABLE,
    }

    /**
     * DatasourceReliabilityStatus is a ENUM which holds the possible statuses
     * to return for checking if a datasource is reliable
     *
     * RELIABLE - states that the datasource is reliable
     * NOT_RELIABLE -  states that the datasource is not reliable
     */
    public enum DatasourceReliabilityStatus {
        /**
         * We set the status as Reliable if the datasource is up and running and it provides information
         */
        RELIABLE,
        /**
         * We set the status as Not Reliable if the datasource is up and not providing information
         */
        NOT_RELIABLE,
    }

    public enum QueryValidity {
        VALID,
        INVALID_PARAMS,
        INVALID_RANGE,
    }
}

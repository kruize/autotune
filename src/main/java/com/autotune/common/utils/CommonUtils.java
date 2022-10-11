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

import com.autotune.utils.AutotuneConstants;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * QueryValidity is an ENUM which holds the possible validity status to be
     * returned after performing a query validation
     */
    public enum QueryValidity {
        // If the query check is done and the query is found to be valid
        VALID,
        // If the query is found to be having invalid parameters
        INVALID_PARAMS,
        // If the query is found to be having invalid time ranges
        INVALID_RANGE,
        // If the query itself is found to be invalid
        INVALID_QUERY,
        // If the query is empty
        EMPTY_QUERY,
        // If the query is null
        NULL_QUERY
    }

    /**
     * Extracts the time value (digits) from a time string
     * @param timestr
     * @return
     */
    public static int getTimeValue(String timestr) {
        String workingstr = timestr.replace(AutotuneConstants.Patterns.WHITESPACE_PATTERN, "");
        Pattern pattern = Pattern.compile(AutotuneConstants.Patterns.DURATION_PATTERN);
        Matcher matcher = pattern.matcher(workingstr);
        if (matcher.find()) {
            if (null != matcher.group(1)) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Extracts the time units (time measurement units eg: seconds, minutes etc)
     * from a time string
     * @param timestr
     * @return
     */
    public static TimeUnit getTimeUnit(String timestr) {
        String workingstr = timestr.replace(AutotuneConstants.Patterns.WHITESPACE_PATTERN, "");
        Pattern pattern = Pattern.compile(AutotuneConstants.Patterns.DURATION_PATTERN);
        Matcher matcher = pattern.matcher(workingstr);
        if (matcher.find()) {
            if (null != matcher.group(2).trim()) {
                String trimmedDurationUnit = matcher.group(2).trim();
                if (trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.SECOND_SINGLE_LC)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.SECOND_SHORT_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.SECOND_SHORT_LC_PLURAL)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.SECOND_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.SECOND_LC_PLURAL)) {
                    return TimeUnit.SECONDS;
                }
                if (trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.MINUTE_SINGLE_LC)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.MINUTE_SHORT_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.MINUTE_SHORT_LC_PLURAL)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.MINUTE_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.MINUTE_LC_PLURAL)) {
                    return TimeUnit.MINUTES;
                }
                if (trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.HOUR_SINGLE_LC)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.HOUR_SHORT_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.HOUR_SHORT_LC_PLURAL)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.HOUR_LC_SINGULAR)
                        || trimmedDurationUnit.equalsIgnoreCase(AutotuneConstants.TimeUnitsExt.HOUR_LC_PLURAL)) {
                    return TimeUnit.HOURS;
                }
            }
        }
        return null;
    }

    /**
     * Returns the short string representation of a time unit
     * @param timeUnit
     * @return
     */
    public static String getShortRepOfTimeUnit(TimeUnit timeUnit) {
        if (timeUnit.equals(TimeUnit.HOURS))
            return AutotuneConstants.TimeUnitsExt.HOUR_SINGLE_LC;
        if (timeUnit.equals(TimeUnit.MINUTES))
            return AutotuneConstants.TimeUnitsExt.MINUTE_SINGLE_LC;
        if (timeUnit.equals(TimeUnit.SECONDS))
            return AutotuneConstants.TimeUnitsExt.SECOND_SINGLE_LC;
        return null;
    }


    /**
     * Converts the time unit to seconds
     * @param unit
     * @return
     */
    public static int getTimeUnitInSeconds(TimeUnit unit) {
        if (unit.equals(TimeUnit.SECONDS)) {
            return 1;
        } else if (unit.equals(TimeUnit.MINUTES)) {
            return AutotuneConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;
        } else if (unit.equals(TimeUnit.HOURS)) {
            return AutotuneConstants.TimeConv.NO_OF_MINUTES_PER_HOUR * AutotuneConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Checks if the query has a time range
     * @param query
     * @return
     */
    public static boolean checkIfQueryHasTimeRange(String query) {
        String workingstr = query.replace(AutotuneConstants.Patterns.WHITESPACE_PATTERN, "");
        Pattern pattern = Pattern.compile(AutotuneConstants.Patterns.QUERY_WITH_TIME_RANGE_PATTERN);
        Matcher matcher = pattern.matcher(workingstr);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    /**
     * Extracts the time range from a query
     * @param query
     * @return
     */
    public static String extractTimeUnitFromQuery(String query) {
        String timeContent = query.substring(query.lastIndexOf('[') + 1);
        timeContent = timeContent.substring(0, timeContent.indexOf(']'));
        return timeContent;
    }

    /**
     * Checks if two time strings represent same time
     * @param timeStrOne
     * @param timeStrTwo
     * @return
     */
    public static boolean checkTimeMatch(String timeStrOne, String timeStrTwo) {
        int timeStrOneTimeValue = CommonUtils.getTimeValue(timeStrOne);
        TimeUnit timeStrOneTimeUnit = CommonUtils.getTimeUnit(timeStrOne);
        int timeStrTwoTimeValue = CommonUtils.getTimeValue(timeStrTwo);
        TimeUnit timeStrTwoTimeUnit = CommonUtils.getTimeUnit(timeStrTwo);
        if (Integer.MIN_VALUE != timeStrOneTimeValue
                && Integer.MIN_VALUE != timeStrTwoTimeValue
                && null != timeStrOneTimeUnit
                && null != timeStrTwoTimeUnit) {
            return timeStrOneTimeValue * getTimeUnitInSeconds(timeStrOneTimeUnit) == timeStrTwoTimeValue * getTimeUnitInSeconds(timeStrTwoTimeUnit);
        }
        return false;
    }

    /**
     * Get the base datasource URL for running query
     * @param url
     * @param datasource
     * @return
     */
    public static String getBaseDataSourceUrl(String url, String datasource) {
        if (datasource.equalsIgnoreCase(AutotuneConstants.SupportedDatasources.PROMETHEUS)) {
            return (new StringBuilder())
                    .append(url)
                    .append("/api/v1/query?query=")
                    .toString();
        }
        return null;
    }
}

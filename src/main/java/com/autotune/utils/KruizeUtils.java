/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * This is a Utility class at the Autotune level for common constants, functions etc.
 * @author bipkumar
 *
 */
public final class KruizeUtils {

	private final static String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	// Initial capacity of the queue
	public static int INITIAL_QUEUE_CAPACITY = 50;

	// Blocking queue names used in Autotune
	public enum Operation {
		ADD, UPDATE, DELETE
	}

	public enum QueueName {
		RECMGRQUEUE, EXPMGRQUEUE
	}

	/**
	 * Generic URL pattern validator for validating the url
	 * @param url as String of type https, ftp, file
	 * @return true if input url is valid else return false
	 */
	public static boolean isValidURL(String url) {

		Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
		if (url == null || url == "") {
			return false;
		}
		Matcher matcher = URL_PATTERN.matcher(url);
		return matcher.matches();
	}

    /**
     * Parses a duration string (e.g., "30 min", "2 hours") into a total number of days.
     *
     * @param durationString The string to parse, like "30 min".
     * @return The equivalent duration in days as a double.
     * @throws IllegalArgumentException if the string format or unit is invalid.
     */
    public static double parseDurationToDays(String durationString) {
        if (durationString == null || durationString.isBlank()) {
            throw new IllegalArgumentException("Duration string cannot be null or empty.");
        }

        // Split the string into number and unit parts
        String[] parts = durationString.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid duration format. Expected: '<value> <unit>' (e.g., '30 min').");
        }

        double value;
        try {
            value = Double.parseDouble(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in duration string: " + parts[0]);
        }

        String unit = parts[1].toLowerCase();
        long minutes;

        // Convert the value to a common base (minutes)
        if (unit.startsWith("min")) {
            minutes = (long) value;
        } else if (unit.startsWith("hour")) {
            minutes = TimeUnit.HOURS.toMinutes((long) value);
        } else if (unit.startsWith("day")) {
            minutes = TimeUnit.DAYS.toMinutes((long) value);
        } else {
            throw new IllegalArgumentException("Unsupported time unit: " + unit + ". Supported units are 'min', 'hour', 'day'.");
        }

        // Convert the total minutes into a fraction of a day
        return (double) minutes / TimeUnit.DAYS.toMinutes(1); // 1 day = 1440 minutes
    }
}

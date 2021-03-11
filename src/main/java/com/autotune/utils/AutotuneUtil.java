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

/**
 * This is a Utility class at the Autotune level for common constants, functions etc.
 * @author bipkumar
 *
 */
public final class AutotuneUtil {
	
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
}

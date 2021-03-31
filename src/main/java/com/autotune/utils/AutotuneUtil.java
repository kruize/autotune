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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

/**
 * This is a Utility class at the Autotune level for common constants, functions etc.
 * @author Bipn Kumar
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
	
	public static JSONObject retriveDataFromURL(String httpsURL) {
		JSONObject jsonObj = null;
		if (!isValidURL(httpsURL)) {
			System.out.println("URL is not valid or null.");
			return null;
		}
		try {
			URL myUrl = new URL(httpsURL);
			HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
			InputStream inStream = conn.getInputStream();

			if (inStream == null) {
				throw new IllegalArgumentException("file not found! ");
			} else {
				String inputJsonStr = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))
						.lines().collect(Collectors.joining("\n"));

				jsonObj = new JSONObject(inputJsonStr);
			}

		} catch (IOException e) {

		}
		return jsonObj;
	}
}

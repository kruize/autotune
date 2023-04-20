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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Contains helper methods for HTTP requests and parsing responses.
 */
public class HttpUtils
{
	private HttpUtils() { }

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

	/**
	 * @param url URL for getting query response from data source
	 * @param bearerToken Authorization token to connect to data source
	 * @return response of query
	 */
	public static String getDataFromURL(URL url, String bearerToken) {
		String result = null;
		try {
			HttpURLConnection connection;

			if (url.getProtocol().equals("https")) {
				connection = (HttpsURLConnection) url.openConnection();
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}

			connection.setRequestProperty("Authorization", bearerToken);

			if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
				result = getDataFromConnection(connection);
			} else {
				if (connection.getResponseCode() == HttpsURLConnection.HTTP_FORBIDDEN) {
					LOGGER.error("Please refresh your auth token");
					System.exit(1);
				}
				LOGGER.error("{} Response Failure for {}", connection.getResponseCode(),
						url.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private static String getDataFromConnection(HttpURLConnection connection) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()
		));

		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = bufferedReader.readLine()) != null) {
			response.append(inputLine);
		}

		bufferedReader.close();
		return response.toString();
	}

	public static void disableSSLVerification() {
		TrustManager[] dummyTrustManager = new TrustManager[]{new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) { }

			public void checkServerTrusted(X509Certificate[] certs, String authType) { }
		}};

		HostnameVerifier allHostsValid = (hostname, session) -> true;

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, dummyTrustManager, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			e.printStackTrace();
		}

		assert sslContext != null;
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	public static String postRequest(URL url, String content) {
		try {
			URLConnection connection = url.openConnection();
			HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setDoOutput(true);

			byte[] out = content.getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			httpURLConnection.setFixedLengthStreamingMode(length);
			httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			httpURLConnection.connect();
			try(OutputStream outputStream = httpURLConnection.getOutputStream()) {
				outputStream.write(out);
			}
			String data = getDataFromConnection(httpURLConnection);
			return data;

		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
}

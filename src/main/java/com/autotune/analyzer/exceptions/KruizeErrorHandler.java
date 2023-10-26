/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.exceptions;

import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * If any error or exception in restapi will be converted into JSON having details like.
 * Response code
 * Error message
 * documentation link
 */
public class KruizeErrorHandler extends ErrorPageErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeErrorHandler.class);

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setMethod("GET");
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        String origMessage = (String) request.getAttribute("javax.servlet.error.message");
        int errorCode = response.getStatus();
        List<UpdateResultsAPIObject> myList = (List<UpdateResultsAPIObject>) request.getAttribute("data");
        PrintWriter out = response.getWriter();
        Gson gsonObj = new GsonBuilder()
                .disableHtmlEscaping()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .create();
        String gsonStr = gsonObj.toJson(new KruizeResponse(origMessage, errorCode, "", "ERROR", myList));
        if (errorCode != HttpServletResponse.SC_CONFLICT) {
            LOGGER.error(gsonStr);
        }
        out.append(gsonStr);
        out.flush();
    }
}

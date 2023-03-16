/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

package com.autotune.utils.filter;

import com.autotune.utils.AutotuneConstants;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the required methods to serve the CORS filter added to server
 */
public class KruizeCORSFilter {
    // Making sure instance/object is not created
    private KruizeCORSFilter() {

    }
    // Initialise the Filter Holder to null
    private static FilterHolder filterHolder = null;

    public static FilterHolder getFilter() {
        // If it's the first call for the filterholder, Create it
        if (null == filterHolder) {
            filterHolder = new FilterHolder(CrossOriginFilter.class);
            Map<String, String> filterParams = new HashMap<String, String>();
            filterParams.put(
                    CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER,
                    AutotuneConstants.CORSConstants.ALLOWED_ORIGINS
            );
            filterParams.put(
                    CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER,
                    AutotuneConstants.CORSConstants.ALLOWED_METHODS
            );
            filterParams.put(
                    CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER,
                    AutotuneConstants.CORSConstants.ALLOWED_HEADERS
            );
            filterParams.put(
                    CrossOriginFilter.ACCESS_CONTROL_MAX_AGE_HEADER,
                    AutotuneConstants.CORSConstants.MAX_AGE
            );
            filterHolder.setInitParameters(filterParams);
        }
        return filterHolder;
    }
}

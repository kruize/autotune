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

package com.autotune.utils.filter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import static com.autotune.utils.KruizeConstants.APIMessages.RATE_LIMIT_EXCEEDED;
import static com.autotune.utils.KruizeConstants.JSONKeys.ERROR;

public class QueueSizeRateLimitFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueSizeRateLimitFilter.class);
    private final BlockingQueue<Runnable> queue;

    public QueueSizeRateLimitFilter(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("QueueSizeRateLimitFilter doFilter queue size: {} ", queue.remainingCapacity());
        if (queue.remainingCapacity() == 0) {
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResp.setContentType("application/json");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(ERROR, RATE_LIMIT_EXCEEDED);
            httpResp.getWriter().write(jsonObject.toString());
            return;
        }

        chain.doFilter(request, response);
    }
}

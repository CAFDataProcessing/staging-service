/*
 * Copyright 2019-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.services.staging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;

public final class LoggingMDCInterceptor implements HandlerInterceptor
{

    private static final String TENANT_HEADER = "X-TENANT-ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingController.class);

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    {
        // Wrap the request to allow reading the body multiple times
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

        final String tenant = request.getHeader(TENANT_HEADER);
        if (StringUtils.isNotEmpty(tenant)) {
            MDC.put("tenantId", tenant);
        }

        LOGGER.info("Request URL: {}", request.getRequestURL());
        LOGGER.info("Request Headers: {}", getRequestHeaders(request));
        LOGGER.info("Request Body: {}", getRequestBody(wrapper));

        return true;
    }

    @Override
    public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
                           final ModelAndView modelAndView)
    {
        MDC.remove(TENANT_HEADER);
    }

    private String getRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(headerName ->
                headers.append(headerName).append(": ").append(request.getHeader(headerName)).append(", ")
        );
        return headers.toString();
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] requestBody = request.getContentAsByteArray();
        try {
            return new String(requestBody, 0, requestBody.length, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Error reading request body", e);
            return "Error reading request body " + e.getMessage();
        }
    }
}

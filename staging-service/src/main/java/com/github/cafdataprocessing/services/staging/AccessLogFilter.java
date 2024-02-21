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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AccessLogFilter implements Filter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {

        if (request instanceof HttpServletRequest) {
            LOGGER.info("doFilter called with HttpServletRequest");
            LOGGER.info("HttpServletRequest getRequestURI: {}", ((HttpServletRequest) request).getRequestURI());
            LOGGER.info("HttpServletRequest getPathInfo: {}", ((HttpServletRequest) request).getPathInfo());
            LOGGER.info("HttpServletRequest getHeaderNames: {}", ((HttpServletRequest) request).getHeaderNames());
            LOGGER.info("HttpServletRequest getMethod: {}", ((HttpServletRequest) request).getMethod());
            LOGGER.info("HttpServletRequest getRemoteUser: {}", ((HttpServletRequest) request).getRemoteUser());
            LOGGER.info("HttpServletRequest getQueryString: {}", ((HttpServletRequest) request).getQueryString());
            LOGGER.info("HttpServletRequest getServletPath: {}", ((HttpServletRequest) request).getServletPath());
            LOGGER.info("HttpServletRequest getAuthType: {}", ((HttpServletRequest) request).getAuthType());
            LOGGER.info("HttpServletRequest getContextPath: {}", ((HttpServletRequest) request).getContextPath());
            LOGGER.info("HttpServletRequest getPathTranslated: {}", ((HttpServletRequest) request).getPathTranslated());

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            LOGGER.info("HttpServletResponse getStatus: {}", httpResponse.getStatus());
            LOGGER.info("HttpServletResponse getHeaderNames: {}", httpResponse.getHeaderNames());
            LOGGER.info("HttpServletResponse getContentType: {}", httpResponse.getContentType());
            LOGGER.info("HttpServletResponse getCharacterEncoding: {}", httpResponse.getCharacterEncoding());

//
//            final String uri = ((HttpServletRequest) request).getRequestURI();
//            //detect the uri who dontt need to be log
//            if (uri.startsWith("/actuator")) {
//                // Add the "No_LOG" Attribute to request, the value is not important, it only needs to be not null
//                request.setAttribute("NO_LOG", "true");
//            }
        } else {
            LOGGER.info("doFilter called with " + request.getClass().getName());

            LOGGER.info("ServletRequest getCharacterEncoding: {}", request.getCharacterEncoding());
            LOGGER.info("ServletRequest getContentType: {}", request.getContentType());
            LOGGER.info("ServletRequest getLocalAddr: {}", request.getLocalAddr());
            LOGGER.info("ServletRequest getLocalName: {}", request.getLocalName());
            LOGGER.info("ServletRequest getLocalPort: {}", request.getLocalPort());
            LOGGER.info("ServletRequest getRemoteAddr: {}", request.getRemoteAddr());
            LOGGER.info("ServletRequest getRemoteHost: {}", request.getRemoteHost());
            LOGGER.info("ServletRequest getRemotePort: {}", request.getRemotePort());
            LOGGER.info("ServletRequest getServerName: {}", request.getServerName());
            LOGGER.info("ServletRequest getServerPort: {}", request.getServerPort());
            LOGGER.info("ServletRequest getScheme: {}", request.getScheme());
            LOGGER.info("ServletRequest getProtocol: {}", request.getProtocol());
            LOGGER.info("ServletRequest getServerName: {}", request.getServerName());
            LOGGER.info("ServletRequest getServerPort: {}", request.getServerPort());

            // Log info from the response object
            LOGGER.info("ServletResponse getCharacterEncoding: {}", response.getCharacterEncoding());
            LOGGER.info("ServletResponse getContentType: {}", response.getContentType());
            LOGGER.info("ServletResponse getLocale: {}", response.getLocale());
            LOGGER.info("ServletResponse isCommitted: {}", response.isCommitted());
        }
        chain.doFilter(request, response);
    }
}
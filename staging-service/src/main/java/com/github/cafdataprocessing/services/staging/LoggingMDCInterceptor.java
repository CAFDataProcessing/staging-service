package com.github.cafdataprocessing.services.staging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class LoggingMDCInterceptor extends HandlerInterceptorAdapter {

    private static final String TENANT_HEADER = "X-TENANT-ID";

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        final String tenant = request.getHeader(TENANT_HEADER);
        if (StringUtils.isNotEmpty(tenant)) {
            MDC.put("tenantId", tenant);
        }
        return true;
    }

    @Override
    public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
                           final ModelAndView modelAndView) {
        MDC.remove(TENANT_HEADER);
    }
}

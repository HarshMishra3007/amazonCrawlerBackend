package com.amazon.productintelligence.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PublicApiCacheInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/api/products")) {
            // Server-side Caffeine cache handles performance; avoid browser caching stale lists after admin deletes.
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
        }
        return true;
    }
}

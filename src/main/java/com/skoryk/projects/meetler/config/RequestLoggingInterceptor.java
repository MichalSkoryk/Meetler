package com.skoryk.projects.meetler.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
  private static final String START_TIME_ATTRIBUTE = "meetler.request.startTime";

  @Override
  public boolean preHandle(
      HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
    request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
    log.info(
        "HTTP {} {} started from {}",
        request.getMethod(),
        request.getRequestURI(),
        request.getRemoteAddr());
    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex) {
    long durationMs = calculateDurationMs(request);

    if (ex == null) {
      log.info(
          "HTTP {} {} finished with status {} in {} ms",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      return;
    }

    log.warn(
        "HTTP {} {} failed with status {} in {} ms",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        durationMs,
        ex);
  }

  private long calculateDurationMs(HttpServletRequest request) {
    Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
    if (startTime instanceof Long startedAt) {
      return System.currentTimeMillis() - startedAt;
    }
    return -1;
  }
}

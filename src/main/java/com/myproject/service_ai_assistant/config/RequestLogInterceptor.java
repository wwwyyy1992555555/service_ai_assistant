package com.myproject.service_ai_assistant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 请求日志拦截器
 */
@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);
        
        // 记录请求信息
        log.info("【请求开始】{} {} | IP: {} | User-Agent: {}", 
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                request.getHeader("User-Agent"));
        
        // 记录请求参数
        String params = getRequestParams(request);
        if (!params.isEmpty()) {
            log.debug("【请求参数】{}", params);
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        
        // 记录响应状态和耗时
        log.info("【请求完成】{} {} | 状态：{} | 耗时：{}ms", 
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        if (ex != null) {
            log.error("【请求异常】{} {} | 异常：{}", 
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage(), ex);
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(HttpServletRequest request) {
        StringBuilder params = new StringBuilder();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.append(key).append("=").append(String.join(",", values)).append("&");
            }
        });
        if (params.length() > 0) {
            params.setLength(params.length() - 1);
        }
        return params.toString();
    }
}

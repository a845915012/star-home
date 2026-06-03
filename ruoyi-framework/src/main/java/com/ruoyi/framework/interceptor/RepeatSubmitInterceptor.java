package com.ruoyi.framework.interceptor;

import java.lang.reflect.Method;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 防止重复提交拦截器
 *
 * @author ruoyi
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor
{
    private static final int DEFAULT_INTERVAL = 5000;

    private static final String DEFAULT_MESSAGE = "不允许重复提交，请稍候再试";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (handler instanceof HandlerMethod)
        {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);

            boolean writeMethod = isWriteMethod(request);
            if (annotation != null || writeMethod)
            {
                int interval = annotation != null ? annotation.interval() : DEFAULT_INTERVAL;
                String message = annotation != null ? annotation.message() : DEFAULT_MESSAGE;
                if (this.isRepeatSubmit(request, interval))
                {
                    AjaxResult ajaxResult = AjaxResult.error(message);
                    ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
                    return false;
                }
            }
            return true;
        }
        else
        {
            return true;
        }
    }

    private boolean isWriteMethod(HttpServletRequest request)
    {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    /**
     * 验证是否重复提交由子类实现具体的防重复提交的规则
     *
     * @param request 请求信息
     * @param interval 防重复间隔时间（毫秒）
     * @return 结果
     * @throws Exception 异常
     */
    public abstract boolean isRepeatSubmit(HttpServletRequest request, int interval);
}

package com.core.reminder.aspect;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ActivityStatus;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.service.UserActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户活动日志AOP切面
 * 自动记录用户的操作行为
 */
@Slf4j
@Aspect
@Component
public class ActivityLogAspect {

    @Autowired
    private UserActivityLogService activityLogService;

    /**
     * 活动日志注解
     * 用于标记需要记录日志的方法
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LogActivity {
        /**
         * 操作类型
         */
        ActivityAction action();

        /**
         * 资源类型
         */
        ResourceType resourceType() default ResourceType.UNKNOWN;

        /**
         * 操作描述
         */
        String description() default "";

        /**
         * 是否异步记录
         */
        boolean async() default true;

        /**
         * 是否记录方法参数
         */
        boolean logParams() default false;

        /**
         * 是否记录返回值
         */
        boolean logResult() default false;
    }

    /**
     * 定义切点：所有带有@LogActivity注解的方法
     */
    @Pointcut("@annotation(logActivity)")
    public void logActivityPointcut(LogActivity logActivity) {}

    /**
     * 环绕通知：记录方法执行前后的日志
     */
    @Around("logActivityPointcut(logActivity)")
    public Object logActivityAround(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();
        Long userId = getCurrentUserId(request);
        
        Object result = null;
        ActivityStatus status = ActivityStatus.SUCCESS;
        String errorMessage = null;
        Map<String, Object> details = new HashMap<>();

        try {
            // 记录方法参数
            if (logActivity.logParams()) {
                details.put("methodParams", getMethodParams(joinPoint));
            }

            // 执行目标方法
            result = joinPoint.proceed();

            // 记录返回值
            if (logActivity.logResult() && result != null) {
                details.put("result", result.toString());
            }

        } catch (Exception e) {
            status = ActivityStatus.FAILED;
            errorMessage = e.getMessage();
            details.put("exception", e.getClass().getSimpleName());
            throw e;
        } finally {
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            details.put("executionTime", executionTime);
            details.put("methodName", joinPoint.getSignature().getName());
            details.put("className", joinPoint.getTarget().getClass().getSimpleName());

            // 提取资源信息
            Long resourceId = extractResourceId(joinPoint, result);
            String resourceName = extractResourceName(joinPoint, result);

            // 记录活动日志
            if (logActivity.async()) {
                activityLogService.logActivityAsync(
                    userId, logActivity.action(), status, logActivity.resourceType(),
                    resourceId, resourceName, errorMessage, details, request
                );
            } else {
                activityLogService.logActivity(
                    userId, logActivity.action(), status, logActivity.resourceType(),
                    resourceId, resourceName, errorMessage, details, request
                );
            }
        }

        return result;
    }

    /**
     * 登录成功后置通知
     */
    @AfterReturning(pointcut = "execution(* com.core.reminder.service.AuthService.loginUser(..))", returning = "result")
    public void logLoginSuccess(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && result != null) {
            // 从登录结果中提取用户ID
            Long userId = extractUserIdFromLoginResult(result);
            activityLogService.logActivityAsync(userId, ActivityAction.LOGIN, ActivityStatus.SUCCESS,
                    ResourceType.USER, userId, null, null, null, request);
        }
    }

    /**
     * 登录失败异常通知
     */
    @AfterThrowing(pointcut = "execution(* com.core.reminder.service.AuthService.loginUser(..))", throwing = "ex")
    public void logLoginFailure(JoinPoint joinPoint, Exception ex) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Map<String, Object> details = new HashMap<>();
            details.put("failureReason", ex.getMessage());
            
            activityLogService.logActivityAsync(null, ActivityAction.LOGIN_FAILED, ActivityStatus.FAILED,
                    ResourceType.USER, null, null, ex.getMessage(), details, request);
        }
    }

    /**
     * 微信登录成功后置通知
     */
    @AfterReturning(pointcut = "execution(* com.core.reminder.service.WechatAuthService.login(..))", returning = "result")
    public void logWechatLoginSuccess(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && result != null) {
            Long userId = extractUserIdFromLoginResult(result);
            activityLogService.logActivityAsync(userId, ActivityAction.WECHAT_LOGIN, ActivityStatus.SUCCESS,
                    ResourceType.USER, userId, null, null, null, request);
        }
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            // 从请求属性中获取当前用户信息
            UserProfileDto currentUser = (UserProfileDto) request.getAttribute("currentUser");
            return currentUser != null ? currentUser.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取方法参数
     */
    private Map<String, Object> getMethodParams(JoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if (args[i] != null) {
                // 避免记录敏感信息
                if (isSensitiveParam(paramNames[i], args[i])) {
                    params.put(paramNames[i], "***");
                } else {
                    params.put(paramNames[i], args[i].toString());
                }
            }
        }

        return params;
    }

    /**
     * 获取参数名称
     */
    private String[] getParameterNames(JoinPoint joinPoint) {
        // 简化实现，实际项目中可以使用反射或字节码工具获取真实参数名
        Object[] args = joinPoint.getArgs();
        String[] names = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            names[i] = "param" + i;
        }
        return names;
    }

    /**
     * 检查是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName, Object value) {
        String lowerParamName = paramName.toLowerCase();
        return lowerParamName.contains("password") || 
               lowerParamName.contains("token") || 
               lowerParamName.contains("secret") ||
               lowerParamName.contains("key");
    }

    /**
     * 提取资源ID
     */
    private Long extractResourceId(JoinPoint joinPoint, Object result) {
        // 尝试从方法参数中提取ID
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        // 尝试从返回值中提取ID
        if (result != null) {
            try {
                if (result.getClass().getMethod("getId") != null) {
                    return (Long) result.getClass().getMethod("getId").invoke(result);
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }

        return null;
    }

    /**
     * 提取资源名称
     */
    private String extractResourceName(JoinPoint joinPoint, Object result) {
        // 尝试从返回值中提取名称
        if (result != null) {
            try {
                if (result.getClass().getMethod("getTitle") != null) {
                    return (String) result.getClass().getMethod("getTitle").invoke(result);
                }
                if (result.getClass().getMethod("getName") != null) {
                    return (String) result.getClass().getMethod("getName").invoke(result);
                }
                if (result.getClass().getMethod("getNickname") != null) {
                    return (String) result.getClass().getMethod("getNickname").invoke(result);
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }

        return null;
    }

    /**
     * 从登录结果中提取用户ID
     */
    private Long extractUserIdFromLoginResult(Object result) {
        try {
            if (result.getClass().getMethod("getUserId") != null) {
                return (Long) result.getClass().getMethod("getUserId").invoke(result);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
}
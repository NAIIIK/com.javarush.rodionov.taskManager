package com.javarush.taskmanager.logging;

import com.javarush.taskmanager.logging.annotation.Sensitive;
import com.javarush.taskmanager.logging.annotation.SensitiveResult;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private static final String MASK = "***";

    @Around("within(@org.springframework.stereotype.Service *)")
    public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String location = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        long startedAt = System.currentTimeMillis();

        log.info("--> {}({})", location, formatArgs(method, joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            log.info("<-- {} [{} ms] returned {}", location, elapsed(startedAt), formatResult(method, result));
            return result;
        } catch (Throwable ex) {
            log.warn("<-x {} [{} ms] threw {}: {}",
                    location, elapsed(startedAt), ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private String formatArgs(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        Parameter[] parameters = method.getParameters();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            boolean sensitive = i < parameters.length && parameters[i].isAnnotationPresent(Sensitive.class);
            sb.append(sensitive ? MASK : args[i]);
        }
        return sb.toString();
    }

    private Object formatResult(Method method, Object result) {
        if (method.getReturnType() == void.class) {
            return "void";
        }
        return method.isAnnotationPresent(SensitiveResult.class) ? MASK : result;
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
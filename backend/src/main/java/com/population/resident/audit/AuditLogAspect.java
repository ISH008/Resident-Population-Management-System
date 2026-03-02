package com.population.resident.audit;

import com.population.resident.domain.SysOperationLog;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import com.population.resident.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        SysOperationLog log = buildBaseLog(auditLog, joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            log.setResult("SUCCESS");
            log.setMessage("操作成功");
            return result;
        } catch (Exception ex) {
            log.setResult("FAIL");
            log.setMessage(limit(ex.getMessage(), 500));
            throw ex;
        } finally {
            auditLogService.save(log);
        }
    }

    private SysOperationLog buildBaseLog(AuditLog auditLog, Object[] args) {
        SysOperationLog log = new SysOperationLog();
        log.setModule(auditLog.module());
        log.setAction(auditLog.action());
        log.setTargetId(extractTargetId(args));

        CurrentUser user = UserContext.get();
        if (user != null) {
            log.setOperatorId(user.getUserId());
            log.setOperatorUsername(user.getUsername());
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(request.getRequestURI());
        }
        return log;
    }

    private String extractTargetId(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        return limit(String.valueOf(args[0]), 64);
    }

    private String limit(String value, int size) {
        if (value == null) {
            return null;
        }
        return value.length() <= size ? value : value.substring(0, size);
    }
}

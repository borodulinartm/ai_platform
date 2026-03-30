package com.huawei.ai_platform.aspect;

import com.huawei.ai_platform.common.annotation.DbLock;
import com.huawei.ai_platform.lock.application.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Locking aspect
 *
 * @author Borodulin Artem
 * @since 2026.03.30
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AspectDbLock {
    private final LockService lockService;

    @Pointcut("@annotation(dbLock)")
    public void dbLockPointcut(DbLock dbLock) {}

    @Around(value = "dbLockPointcut(dbLock)", argNames = "joinPoint,dbLock")
    public Object onAspectRun(ProceedingJoinPoint joinPoint, DbLock dbLock) throws Exception {
        try {
            log.info("Lock guard executing for '{}'", dbLock.category());

            if (StringUtils.isBlank(dbLock.category())) {
                log.error("Cannot execute lock guard because category is null or blank. Returning");
                return null;
            }

            if (!lockService.isLockedFor(dbLock.category())) {
                lockService.lockFor(dbLock.category());
                Object result = joinPoint.proceed();
                lockService.unlockFor(dbLock.category());

                return result;
            } else {
                log.warn("For category '{}' locking are enabled. The method will be not executed!!!", dbLock.category());
            }
        } catch (Throwable e) {
            lockService.unlockFor(dbLock.category());
            log.error("An exception with message: {}", e.getMessage());
        } finally {
            log.info("Lock guard executing has finished for '{}'", dbLock.category());
        }

        return null;
    }
}

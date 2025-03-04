package com.mahitotsu.steropes.api.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(retryFor = CannotAcquireLockException.class, maxAttempts = 10, backoff = @Backoff(delay = 300, maxDelay = 10000, multiplier = 1.5, random = true))
public @interface DsqlRetry {
    
}

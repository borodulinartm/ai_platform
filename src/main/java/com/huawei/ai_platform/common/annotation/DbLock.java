package com.huawei.ai_platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for locking execution method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DbLock {
    /**
     * Specifies table type
     *
     * @return table type
     */
    String category() default "DEFAULT";
}

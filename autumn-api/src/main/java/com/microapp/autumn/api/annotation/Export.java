package com.microapp.autumn.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * export service
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Export {
    String name() default "";
}

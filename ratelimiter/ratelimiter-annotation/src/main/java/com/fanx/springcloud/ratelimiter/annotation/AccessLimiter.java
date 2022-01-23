package com.fanx.springcloud.ratelimiter.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface AccessLimiter {
    String methodKey() default "";
    int limit();
}

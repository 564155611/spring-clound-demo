package com.fanx.springcloud.ratelimiter.annotation;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class AccessLimiterAspect {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private DefaultRedisScript<Boolean> rateLimitLua;

    @Autowired


    @Pointcut("@annotation(com.fanx.springcloud.ratelimiter.annotation.AccessLimiter)")
    void pointcut(){}
    @Before("pointcut()")
    public void before(JoinPoint jp){
        // 1.获得方法签名,作为methodKey
        MethodSignature signature = (MethodSignature) jp.getSignature();

        Method method = signature.getMethod();
        AccessLimiter annotation = method.getAnnotation(AccessLimiter.class);
        String key = annotation.methodKey();
        if (StringUtils.isEmpty(key)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null) {
                String paramterTypeStr = Arrays.stream(parameterTypes)
                        .map(Class::getName)
                        .collect(Collectors.joining(","));
                log.info("parameter types: {}",parameterTypes);
                key= String.format("%s#%s", method.getName(), paramterTypeStr);
            }
        }
        Integer limit = annotation.limit();


        // 2.调用redis lua
        //step 1 : request LUA script
        boolean acquired = stringRedisTemplate.execute(
                rateLimitLua, //传递lua script
                Lists.newArrayList(key), //被限流的key列表
                limit.toString() //传递对应的value列表:限流参数
        );
        if (!acquired) {
            log.error("Your access is blocked,key={}", key);
            throw new RuntimeException("Your access is blocked");
        }
    }
}

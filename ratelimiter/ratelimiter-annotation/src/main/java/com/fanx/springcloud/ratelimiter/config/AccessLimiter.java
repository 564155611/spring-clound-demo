package com.fanx.springcloud.ratelimiter.config;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Deprecated
public class AccessLimiter {
    @Autowired
    private  StringRedisTemplate stringRedisTemplate;
    @Autowired
    private  RedisScript<Boolean> rateLimitLua;

    public void limitAccess(String key, Integer limit) {
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

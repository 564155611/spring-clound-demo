package com.fanx.springcloud.ratelimiter.controller;

import com.fanx.springcloud.ratelimiter.config.AccessLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {
    @Autowired
    AccessLimiter accessLimiter;
    @GetMapping("test")
    @com.fanx.springcloud.ratelimiter.annotation.AccessLimiter(limit=1)
    public String test(){
        //accessLimiter.limitAccess("ratelimiter-annotation-test",1);
        return "success";
    }
}

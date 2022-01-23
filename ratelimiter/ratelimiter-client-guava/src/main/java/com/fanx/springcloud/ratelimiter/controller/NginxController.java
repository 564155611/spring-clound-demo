package com.fanx.springcloud.ratelimiter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class NginxController {
    @GetMapping("nginx")
    public String nginx(){
        log.info("访问nginx");
        return "success";
    }

    @GetMapping("nginx-conn")
    public String nginxConn(@RequestParam(defaultValue = "0") int secs){
        try {
            TimeUnit.SECONDS.sleep(secs);
        } catch (InterruptedException e) {
        }
        return "success";
    }
}

package com.fanx.springcloud.ratelimiter.controller;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class HelloController {
    RateLimiter limiter = RateLimiter.create(2.0);

    /**
     * 为什么限流是2r/s,而在最开始能够出现这样的结果:1秒内出现3次成功?
     *  请求:http://localhost:8080/tryAcquire?count=2
     *  2022-01-19 18:06:33.014: success, rate is 2.0
     *  2022-01-19 18:06:33.622: success, rate is 2.0
     *  2022-01-19 18:06:34.228: success, rate is 2.0
     * 原因:预热模型
     *  对于漏桶算法而言(SmoothBursty):最开始会调用setRate(2)->doSetRate方法,
     *          storedPermits(当前已经预热的令牌数)=maxPermits=1.0*2=2.0,
     *          nextFreeTicketMicros(下次可获取令牌的时间)=nowMicros
     *          stableIntervalMicros(时间间隔)=1/2=0.5
     *  第一次访问获取令牌时
     *      ①调用canAcquire方法:nextFreeTicketMicros-timeoutMicros<=nowMicros返回true,所以本次请求可以成功
     *      ②调用resync(nowMicros):storedPermits=(nowMicros-nextFreeTicketMicros)/stableIntervalMicros)
     *      +storedPermits=0+2.0=2.0,nextFreeTicketMicros=nowMicros
     *      ③扣减操作:从storedPermits里边扣减2,由于SmoothBursty从storedPermits(size=2)中扣减2令牌不需要等待,
     *      而且也没有欠任何的令牌,所以waitTime=0+0=0,所以nextFreeTicketMicros += waitTime的结果保持不变
     *  第二次获取令牌时,由于第二次请求与第一次请求之间间隔500ms,而第一次请求后得到nextFreeTicketMicros=nowMicros
     *  所以:nowMicros=nextFreeTicketMicros+0.5s
     *      ①调用canAcquire方法,由于nowMicros=nextFreeTicketMicros+0.5s,所以canAcquire
     *      方法返回nextFreeTicketMicros-timeoutMicros<=nowMicros的结果为true,所以本次仍然能够请求成功.
     *      ②resync(nowMicros):
     *      storedPermits=storedPermits+(nowMicros-nextFreeTicketMicros)/stableIntervalMicros)=0+0.5/0.5=1,
     *      nextFreeTicketMicros=nowMicros. 此时得到"还欠下的令牌数量"为freshPermits=2-1=1,所以还需要等待的时间
     *      waitTime=freshPermits*stableIntervalMicros=1*0.5=0.5,所以nextFreeTicketMicros=nextFreeTicketMicros
     *      +waitTime=nowMicros+0.5
     *
     *  第三次获取令牌时,由于第三次请求与第二次请求之间间隔500ms,而第二次结果中得到nextFreeTicketMicros=nowMicros+0.5s
     *  所以:nowMicros=nextFreeTicketMicros
     *      ①调用canAcquire方法,由于nowMicros=nextFreeTicketMicros,所以canAcquire
     *      方法返回nextFreeTicketMicros-timeoutMicros<=nowMicros的结果为true,所以本次仍然能够请求成功.
     *      ②resync(nowMicros):由于第三次请求与第二次请求之间间隔0ms,所以:
     *      storedPermits=storedPermits+(nowMicros-nextFreeTicketMicros)/stableIntervalMicros)=0+0=0,
     *      nextFreeTicketMicros=nowMicros. 此时得到"还欠下的令牌数量"为freshPermits=2-0=0,所以还需要等待的时间
     *      waitTime=freshPermits*stableIntervalMicros=2*0.5=1s,所以nextFreeTicketMicros=nextFreeTicketMicros
     *      +waitTime=nowMicros+1s
     *  由于nextFreeTicketMicros=nowMicros+1s,而第四次请求只是间隔了500ms,所以canAcquire返回false,将会被限流,
     *  而第五次请求又再次间隔500ms,所以刚好500ms+500ms=1s,满足了canAcquire的约束所以请求成功.
     * 非阻塞限流
     * @param count
     * @return
     */
    @GetMapping("/tryAcquire")
    public String tryAcquire(Integer count) {
        if (limiter.tryAcquire(count)) {
            log.info("success, rate is {}", limiter.getRate());
            return "success";
        }
        log.info("fail, rate is {}", limiter.getRate());
        return "fail";
    }

    /**
     * 限定时间的非阻塞限流(如果RateLimiter判断timeout对应的时间内可以拿到需要的令牌,
     * 则等待直到获取令牌,并返回true;如果不能拿到,则直接非阻塞返回false)
     * @param count   一次请求获取令牌的数量
     * @param timeout 超时时间
     * @return text/plain
     */
    @GetMapping("/tryAcquireWithTimeout")
    public String tryAcquireWithTimeout(Integer count, Integer timeout) {

        if (limiter.tryAcquire(count, timeout, TimeUnit.SECONDS)) {
            log.info("success, rate is {}", limiter.getRate());
            return "success";
        }
        log.info("fail, rate is {}", limiter.getRate());
        return "fail";
    }

    /**
     * 同步阻塞限流
     * @param count   一次请求获取令牌的数量
     * @return
     */
    @GetMapping("/acquire")
    public String acquire(Integer count) {
        try {
            limiter.acquire(count);
            log.info("success, rate is {}", limiter.getRate());
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("fail, rate is {}", limiter.getRate());
            return "fail";
        }
    }
}

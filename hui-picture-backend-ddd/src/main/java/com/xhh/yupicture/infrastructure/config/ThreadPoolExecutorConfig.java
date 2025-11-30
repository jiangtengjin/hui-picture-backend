package com.xhh.yupicture.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * 自定义线程池
     * @return 线程池
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        return new ThreadPoolExecutor(
                10,
                20,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10));
    }

}

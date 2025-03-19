package com.yupi.springbootinit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConfigurationProperties(prefix = "spring.threadpool")
@Data
public class ThreadPoolExecutorConfig {
    private int corePoolSize;
    private int maximumPoolSize;
    private int keepAliveTime;
    private int queueCapacity;
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactory(){
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Thread"+count);
                count++;
                return thread;
            }
        };

        ThreadPoolExecutor threadPoolExecutor =new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity)

        );
        return threadPoolExecutor;
    }
}

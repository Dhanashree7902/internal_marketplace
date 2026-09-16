package com.internalmarketplace.api.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Backs @Async (the notification-fan-out listeners in the notification
 * module, which replace the Node app's Firestore-triggered Cloud Functions)
 * with a virtual-thread-per-task executor, so background notification work
 * never competes with request-handling platform threads.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    private static final class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Async notification listener {} failed", method, ex);
        }
    }
}

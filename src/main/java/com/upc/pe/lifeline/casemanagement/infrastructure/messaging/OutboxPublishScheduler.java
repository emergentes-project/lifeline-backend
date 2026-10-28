package com.upc.pe.lifeline.casemanagement.infrastructure.messaging;

import com.upc.pe.lifeline.casemanagement.application.internal.commandservices.OutboxPublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxPublisherProperties.class)
public class OutboxPublishScheduler {
    @Bean
    ThreadPoolTaskScheduler outboxTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-publisher-");
        scheduler.setDaemon(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    OutboxPublishLoop outboxPublishLoop(ThreadPoolTaskScheduler outboxTaskScheduler,
                                        OutboxPublisher publisher,
                                        OutboxPublisherProperties properties) {
        return new OutboxPublishLoop(outboxTaskScheduler, publisher, properties.publishDelay());
    }

    static final class OutboxPublishLoop {
        private final ThreadPoolTaskScheduler scheduler;
        private final OutboxPublisher publisher;
        private final Duration delay;

        OutboxPublishLoop(ThreadPoolTaskScheduler scheduler, OutboxPublisher publisher, Duration delay) {
            this.scheduler = scheduler;
            this.publisher = publisher;
            this.delay = delay;
        }

        @PostConstruct
        void start() {
            scheduler.scheduleWithFixedDelay(publisher::publishPending, Instant.now().plus(delay), delay);
        }
    }
}

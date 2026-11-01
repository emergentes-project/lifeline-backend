package com.upc.pe.lifeline.notifications.infrastructure.scheduling;

import com.upc.pe.lifeline.notifications.application.internal.commandservices.NotificationDeliveryService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;

@Configuration(proxyBeanMethods = false)
public class NotificationDeliveryScheduler {
    @Bean
    ThreadPoolTaskScheduler notificationDeliveryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notification-delivery-");
        scheduler.setDaemon(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    NotificationDeliveryLoop notificationDeliveryLoop(
            @Qualifier("notificationDeliveryTaskScheduler") ThreadPoolTaskScheduler notificationDeliveryTaskScheduler,
                                                      NotificationDeliveryService deliveries,
                                                      @Value("${lifeline.notifications.delivery.poll-delay:PT2S}") Duration pollDelay,
                                                      @Value("${lifeline.notifications.delivery.scheduler-enabled:true}") boolean enabled) {
        return new NotificationDeliveryLoop(notificationDeliveryTaskScheduler, deliveries, pollDelay, enabled);
    }

    static final class NotificationDeliveryLoop {
        private final ThreadPoolTaskScheduler scheduler;
        private final NotificationDeliveryService deliveries;
        private final Duration delay;
        private final boolean enabled;

        NotificationDeliveryLoop(ThreadPoolTaskScheduler scheduler, NotificationDeliveryService deliveries,
                                 Duration delay, boolean enabled) {
            this.scheduler = scheduler;
            this.deliveries = deliveries;
            this.delay = delay;
            this.enabled = enabled;
        }

        @PostConstruct
        void start() {
            if (!enabled) {
                return;
            }
            scheduler.scheduleWithFixedDelay(deliveries::dispatchDue, Instant.now().plus(delay), delay);
        }
    }
}

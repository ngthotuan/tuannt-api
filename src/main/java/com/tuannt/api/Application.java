package com.tuannt.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;


@Slf4j
@ConfigurationPropertiesScan
@SpringBootApplication
public class Application implements ApplicationListener<ContextClosedEvent> {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Service is shutting down...");
        // add shutdown logic here
    }
}

package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BeamApplication {
    private static final Logger logger = LoggerFactory.getLogger(BeamApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BeamApplication.class, args);
        logger.info("BEAM Server started successfully on port 8080");
    }
}
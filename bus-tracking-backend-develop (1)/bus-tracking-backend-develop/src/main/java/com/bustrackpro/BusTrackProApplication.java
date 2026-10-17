package com.bustrackpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main class for the application.
 */
@SpringBootApplication
@EnableAsync
public class BusTrackProApplication {

    public static void main(String[] args) {
        System.setProperty("server.servlet.context-path", "/api");
        SpringApplication.run(BusTrackProApplication.class, args);
    }

}

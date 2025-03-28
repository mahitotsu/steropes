package com.mahitotsu.steropes.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class Main {

    public static void main(final String ...args) {
        SpringApplication.run(Main.class, args);
    }
}
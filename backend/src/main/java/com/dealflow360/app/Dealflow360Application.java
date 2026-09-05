package com.dealflow360.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.dealflow360",
        exclude = UserDetailsServiceAutoConfiguration.class)
public class Dealflow360Application {

    public static void main(String[] args) {
        SpringApplication.run(Dealflow360Application.class, args);
    }
}
package com.ssafy.ssafy_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SsafyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsafyProjectApplication.class, args);
    }

}

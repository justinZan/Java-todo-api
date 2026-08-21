package com.zading.todoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
public class TodoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApiApplication.class, args);
    }
}

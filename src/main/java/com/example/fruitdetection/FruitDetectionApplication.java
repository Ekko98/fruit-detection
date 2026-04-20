package com.example.fruitdetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.fruitdetection"})
public class FruitDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FruitDetectionApplication.class, args);
    }
}
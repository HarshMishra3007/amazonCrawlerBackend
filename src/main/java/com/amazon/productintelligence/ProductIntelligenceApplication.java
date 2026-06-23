package com.amazon.productintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class ProductIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductIntelligenceApplication.class, args);
    }
}

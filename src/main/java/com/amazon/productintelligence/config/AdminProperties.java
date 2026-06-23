package com.amazon.productintelligence.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private String username = "admin";
    private String password = "admin";
}

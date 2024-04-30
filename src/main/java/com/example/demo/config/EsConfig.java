package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix="elasticsearch")
@Configuration
@Data
public class EsConfig {
    private String hosts;
    private String cluster;
    private String index;
}

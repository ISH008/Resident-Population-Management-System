package com.population.resident;

import com.population.resident.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ResidentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResidentManagementApplication.class, args);
    }
}

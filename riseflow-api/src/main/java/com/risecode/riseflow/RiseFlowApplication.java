package com.risecode.riseflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
@EnableJpaRepositories(basePackages = "com.risecode.riseflow")
@SpringBootApplication(scanBasePackages = "com.risecode.riseflow")
public class RiseFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiseFlowApplication.class, args);
    }
}

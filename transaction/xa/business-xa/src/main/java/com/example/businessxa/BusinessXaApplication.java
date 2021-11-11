package com.example.businessxa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class BusinessXaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessXaApplication.class, args);
    }

}

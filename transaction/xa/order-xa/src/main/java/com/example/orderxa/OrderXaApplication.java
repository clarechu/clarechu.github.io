package com.example.orderxa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrderXaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderXaApplication.class, args);
    }

}

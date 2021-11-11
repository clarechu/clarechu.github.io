package com.example.storagexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StorageXaApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageXaApplication.class, args);
    }

}

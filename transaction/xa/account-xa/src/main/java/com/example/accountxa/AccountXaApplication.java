package com.example.accountxa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableDiscoveryClient
//@EnableEurekaClient
@EnableFeignClients
@MapperScan({"com.example.accountxa.dao"})
public class AccountXaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountXaApplication.class, args);
    }

}

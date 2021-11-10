package com.example.accountxa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class AccountXaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountXaApplication.class, args);
    }

}

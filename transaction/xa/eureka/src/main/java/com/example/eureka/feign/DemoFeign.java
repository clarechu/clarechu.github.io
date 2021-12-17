package com.example.eureka.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "eureka-provider")
public interface DemoFeign {

    @GetMapping("/provider/hello")
    String hello();
}

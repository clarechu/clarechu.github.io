package com.example.eureka.controller;

import com.example.eureka.feign.DemoFeign;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping("/consumer")
public class ConsumerController {

    @Resource
    DemoFeign demoFeign;

    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    public String hello() {
        String res = demoFeign.hello();
        return "consumer " + res;
    }

}

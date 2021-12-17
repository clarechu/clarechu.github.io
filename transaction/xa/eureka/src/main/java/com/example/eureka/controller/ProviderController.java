package com.example.eureka.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/provider")
public class ProviderController {

    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    public String hello() {

        return "---> provider";
    }

}

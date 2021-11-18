package com.example.businessxa.controller;


import com.example.businessxa.service.BusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Random;

@RestController
public class BusinessController {

    @Value("${version}")
    private String Version;

    @Value("${spring.application.name}")
    private String Name;

    static final Logger log =
            LoggerFactory.getLogger(BusinessController.class);

    @Autowired
    private BusinessService businessService;

    @RequestMapping(value = "/purchase", method = RequestMethod.GET, produces = "application/json")
    public String purchase(@RequestParam(value = "rollback") Boolean rollback, @RequestParam(value = "count") Integer count) {
        int orderCount = 30;
        if (count != null) {
            orderCount = count;
        }
        try {
            businessService.purchase("TestDatas.USER_ID", "TestDatas.COMMODITY_CODE", orderCount);
        } catch (Exception exx) {
            return "Purchase Failed:" + exx.getMessage();
        }
        return "SUCCESS";
    }


    @RequestMapping(value = "health", method = RequestMethod.GET)
    public Object health() {
        log.debug("health start");
        String message = "";
        try {
            message = businessService.health();
        } catch (Exception exx) {
            exx.printStackTrace();
            return exx.getMessage();
        }
        log.debug("health end");
        message = " ====> "+ Name + "-" + Version + message;
        return message;
    }
}
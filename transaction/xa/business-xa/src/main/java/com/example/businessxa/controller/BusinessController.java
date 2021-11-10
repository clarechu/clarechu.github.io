package com.example.businessxa.controller;


import com.example.businessxa.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusinessController {

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
}
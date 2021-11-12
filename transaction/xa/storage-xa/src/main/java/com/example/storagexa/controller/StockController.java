package com.example.storagexa.controller;


import com.example.storagexa.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    final static Logger log = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockService stockService;

    @RequestMapping(value = "/deduct", method = RequestMethod.GET, produces = "application/json")
    public String deduct(String commodityCode, int count) {
        try {
            stockService.deduct(commodityCode, count);
        } catch (Exception exx) {
            exx.printStackTrace();
            return "FAIL";
        }
        return "SUCCESS";
    }

    @RequestMapping(value = "health", method = RequestMethod.GET)
    public Object health() {
        log.debug("health start");
        log.debug("health end");
        return "SUCCESS";
    }
}
package com.example.accountxa.controller;


import com.example.accountxa.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {
    static final String FAIL = "fail";

    static final String SUCCESS = "SUCCESS";

    static final Logger log = LoggerFactory.getLogger(AccountController.class);
    @Autowired
    private AccountService accountService;

    @RequestMapping(value = "/reduce", method = RequestMethod.GET, produces = "application/json")
    public String reduce(String userId, int money) {
        try {
            accountService.reduce(userId, money);
        } catch (Exception exx) {
            exx.printStackTrace();
            return FAIL;
        }
        return SUCCESS;
    }

    @RequestMapping(value = "health", method = RequestMethod.GET)
    public Object health() {
        log.debug("health start");
        log.debug("health end");
        return "SUCCESS";
    }
}


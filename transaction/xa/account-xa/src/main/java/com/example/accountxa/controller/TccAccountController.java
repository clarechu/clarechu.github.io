package com.example.accountxa.controller;

import com.example.accountxa.service.FirstTccAction;
import com.example.accountxa.service.SecondTccAction;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class TccAccountController {

    @Autowired
    private FirstTccAction firstTccAction;

    @Autowired
    private SecondTccAction secondTccAction;

    @GetMapping(value = "/tc")
    @GlobalTransactional
    public Object tc(@RequestParam("from") String accountNo, @RequestParam("to") String toAccountNo, @RequestParam("amount") double amount) {
        Boolean first = firstTccAction.prepare(null, accountNo, amount);
        secondTccAction.prepare(null, toAccountNo, amount);

        return null;
    }
}

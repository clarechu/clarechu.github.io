package com.example.businessxa.service.impl;

import com.example.businessxa.service.BusinessService;
import com.example.businessxa.feign.OrderService;
import com.example.businessxa.feign.StorageService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BusinessServiceImpl implements BusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessService.class);


    @Autowired
    private StorageService storageService;

    @Autowired
    private OrderService orderService;


    /**
     * 采购
     */
    @Override
    @GlobalTransactional
    public void purchase(String userId, String commodityCode, int orderCount) {
        String xid = RootContext.getXID();
        RootContext.bind(xid);
        LOGGER.info("New Transaction Begins: " + xid);
        storageService.deduct(commodityCode, orderCount);

        orderService.create(userId, commodityCode, orderCount);
    }

    @Override
    public String health() {
        String message = "";
        message = storageService.health();

        message += orderService.health();
        return message;
    }
}

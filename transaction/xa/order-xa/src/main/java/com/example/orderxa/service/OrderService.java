package com.example.orderxa.service;

import com.example.orderxa.feign.AccountFeignClient;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";

    @Value("${version}")
    private String Version;

    @Value("${spring.application.name}")
    private String Name;


    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void create(String userId, String commodityCode, Integer count) {
        String xid = RootContext.getXID();
        LOGGER.info("create order in transaction: " + xid);

        // 定单总价 = 订购数量(count) * 商品单价(100)
        int orderMoney = count * 100;
        // 生成订单
        jdbcTemplate.update("insert order_tbl(user_id,commodity_code,count,money) values(?,?,?,?)",
                new Object[]{userId, commodityCode, count, orderMoney});
        // 调用账户余额扣减
        String result = accountFeignClient.reduce(userId, orderMoney);
        if (!SUCCESS.equals(result)) {
            throw new RuntimeException("Failed to call Account Service. ");
        }

    }

    public String health() {
        LOGGER.info("order health ...");
        String message = accountFeignClient.health();
        return  " ====> " + Name + "-" + Version + message;
    }

}
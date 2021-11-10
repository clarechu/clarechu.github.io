package com.example.businessxa.feign;

import com.example.api.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${application.feign.client.order.name}", url = "${application.feign.client.order.url}")
public interface OrderService {

    /**
     * 创建订单
     */
    @GetMapping("/create")
    Order create(@RequestParam("userId") String userId,
                 @RequestParam("commodityCode") String commodityCode,
                 @RequestParam("orderCount") int orderCount);
}
package com.example.businessxa.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@FeignClient(name = "${application.feign.client.storage.name}", url = "${application.feign.client.storage.url}")
@FeignClient(name = "${application.feign.client.storage.name}")
public interface StorageService {

    /**
     * 扣除存储数量
     */
    @GetMapping("/deduct")
    void deduct(@RequestParam("commodityCode") String commodityCode,
                @RequestParam("count") int count);

    @GetMapping("/health")
    void health();
}
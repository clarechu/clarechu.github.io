package com.example.businessxa.service;

public interface BusinessService {
    /**
     * 采购
     */
    public void purchase(String userId,
                         String commodityCode,
                         int orderCount);
    void health();
}

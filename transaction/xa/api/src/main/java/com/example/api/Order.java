package com.example.api;


import lombok.Data;

@Data
public class Order {
    private String UserId;
    private String CommodityCode;
    private int Count;
    private int Money;
}

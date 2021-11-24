package com.example.api;

import lombok.Data;

/**
 * 账户
 *
 * @author zhangsen
 */
@Data
public class Account {

    private Integer id;

    public Account() {
    }

    /**
     * 账户
     */
    private String accountNo;

    private String uid;

    /**
     * 余额
     */
    private double amount;
    /**
     * 冻结金额
     */
    private double freezedAmount;

}

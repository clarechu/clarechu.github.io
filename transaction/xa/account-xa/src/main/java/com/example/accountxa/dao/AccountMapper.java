package com.example.accountxa.dao;

import com.example.api.Account;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 余额账户 DAO
 */

@Mapper
public interface AccountMapper {

    void addAccount(Account account);

    int updateAmount(Account account);

    int updateFreezedAmount(Account account);

    Account getAccountForUpdate(String accountNo);

    List<Account> list();
}

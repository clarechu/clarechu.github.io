package com.example.accountxa;

import com.example.accountxa.dao.AccountMapper;
import com.example.accountxa.service.impl.AccountServiceImpl;
import com.example.api.Account;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(value = SpringRunner.class)
public class SpringbootMyBatisDemoApplicationTests {

    @Autowired
    private AccountMapper accountMapper;

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Test
    public void findAllUsers() {
        List<Account> users = accountMapper.list();
        log.info("users : {}", users);
        Assert.assertEquals(users.size(), 2);
    }


}

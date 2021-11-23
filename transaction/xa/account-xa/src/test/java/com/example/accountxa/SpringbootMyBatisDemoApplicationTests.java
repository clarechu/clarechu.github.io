package com.example.accountxa;

import com.example.accountxa.dao.AccountMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringbootMyBatisDemoApplicationTests {
    @Autowired
    private AccountMapper userMapper;

    @Test
    public void findAllUsers() {

    }


}

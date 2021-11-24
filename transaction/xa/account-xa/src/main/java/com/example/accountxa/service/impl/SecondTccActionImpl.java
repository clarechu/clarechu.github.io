package com.example.accountxa.service.impl;

import com.example.accountxa.dao.AccountMapper;
import com.example.accountxa.service.SecondTccAction;
import com.example.api.Account;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SecondTccActionImpl implements SecondTccAction {

    @Resource
    private AccountMapper fromAccountDAO;

    static final Logger log = LoggerFactory.getLogger(SecondTccActionImpl.class);
    @Override
    public Boolean prepare(BusinessActionContext businessActionContext, String accountNo, double amount) {
        //分布式事务ID
        final String xid = businessActionContext.getXid();
        Account account = fromAccountDAO.getAccountForUpdate(accountNo);
        if (account == null) {
            throw new RuntimeException("账户不存在");
        }
        //冻结转账金额
        double freezedAmount = account.getFreezedAmount() + amount;
        account.setFreezedAmount(freezedAmount);
        fromAccountDAO.updateFreezedAmount(account);
        System.out.printf("prepareMinus account[%s] amount[%f], dtx transaction id: %s.%n", accountNo, amount, xid);
        return null;
    }

    @Override
    public Boolean commit(BusinessActionContext businessActionContext) {
        log.info("second commit tcc xid:{}", businessActionContext.getXid());
        //分布式事务ID
        final String xid = businessActionContext.getXid();
        //账户ID
        final String accountNo = String.valueOf(businessActionContext.getActionContext("accountNo"));
        //转出金额
        final double amount = Double.valueOf(String.valueOf(businessActionContext.getActionContext("amount")));
        //扣除账户余额
        //释放账户 冻结金额
        Account account = fromAccountDAO.getAccountForUpdate(accountNo);
        double newAmount = account.getAmount() + amount;
        account.setAmount(newAmount);
        //释放账户 冻结金额
        account.setFreezedAmount(0);
        fromAccountDAO.updateAmount(account);
        System.out.println(String.format("minus account[%s] amount[%f], dtx transaction id: %s.", accountNo, amount, xid));
        return true;
    }

    @Override
    public Boolean rollback(BusinessActionContext businessActionContext) {
        log.info("second rollback tcc xid:{}", businessActionContext.getXid());
        //分布式事务ID
        final String xid = businessActionContext.getXid();
        //账户ID
        final String accountNo = String.valueOf(businessActionContext.getActionContext("accountNo"));
        //转出金额
        final double amount = Double.valueOf(String.valueOf(businessActionContext.getActionContext("amount")));
        Account account = fromAccountDAO.getAccountForUpdate(accountNo);
        if (account == null) {
            //账户不存在，回滚什么都不做
            return true;
        }
        //释放冻结金额
        account.setFreezedAmount(0);
        fromAccountDAO.updateFreezedAmount(account);
        System.out.println(String.format("Undo prepareMinus account[%s] amount[%f], dtx transaction id: %s.", accountNo, amount, xid));
        return true;
    }
}

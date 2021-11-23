package com.example.accountxa.service.impl;

import com.example.accountxa.dao.AccountMapper;
import com.example.accountxa.service.FirstTccAction;
import com.example.api.Account;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class FirstTccActionImpl implements FirstTccAction {

    /**
     * 扣钱账户 DAO
     */
    @Resource
    private AccountMapper fromAccountDAO;

    /**
     * 一阶段准备，冻结 转账资金
     *
     * @param businessActionContext
     * @param accountNo
     * @param amount
     * @return
     */
    @Override
    public boolean prepareMinus(BusinessActionContext businessActionContext, final String accountNo, final double amount) {
        //分布式事务ID
        final String xid = businessActionContext.getXid();

        //校验账户余额
        //冻结转账金额
        try {
            //校验账户余额
            Account account = fromAccountDAO.getAccountForUpdate(accountNo);
            if (account == null) {
                throw new RuntimeException("账户不存在");
            }
            if (account.getAmount() - amount < 0) {
                throw new RuntimeException("余额不足");
            }
            //冻结转账金额
            double freezedAmount = account.getFreezedAmount() + amount;
            account.setFreezedAmount(freezedAmount);
            fromAccountDAO.updateFreezedAmount(account);
            System.out.printf("prepareMinus account[%s] amount[%f], dtx transaction id: %s.%n", accountNo, amount, xid);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * 二阶段提交
     *
     * @param businessActionContext
     * @return
     */
    @Override
    public boolean commit(BusinessActionContext businessActionContext) {
        //分布式事务ID
        final String xid = businessActionContext.getXid();
        //账户ID
        final String accountNo = String.valueOf(businessActionContext.getActionContext("accountNo"));
        //转出金额
        final double amount = Double.valueOf(String.valueOf(businessActionContext.getActionContext("amount")));
        //扣除账户余额
        //释放账户 冻结金额
        try {
            Account account = fromAccountDAO.getAccountForUpdate(accountNo);
            //扣除账户余额
            double newAmount = account.getAmount() - amount;
            if (newAmount < 0) {
                throw new RuntimeException("余额不足");
            }
            account.setAmount(newAmount);
            //释放账户 冻结金额
            account.setFreezedAmount(account.getFreezedAmount() - amount);
            fromAccountDAO.updateAmount(account);
            System.out.println(String.format("minus account[%s] amount[%f], dtx transaction id: %s.", accountNo, amount, xid));
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * 二阶段回滚
     *
     * @param businessActionContext
     * @return
     */
    @Override
    public Boolean rollback(BusinessActionContext businessActionContext) {
        //分布式事务ID
        final String xid = businessActionContext.getXid();
        //账户ID
        final String accountNo = String.valueOf(businessActionContext.getActionContext("accountNo"));
        //转出金额
        final double amount = Double.valueOf(String.valueOf(businessActionContext.getActionContext("amount")));
        try {
            Account account = fromAccountDAO.getAccountForUpdate(accountNo);
            if (account == null) {
                //账户不存在，回滚什么都不做
                return true;
            }
            //释放冻结金额
            account.setFreezedAmount(account.getFreezedAmount() - amount);
            fromAccountDAO.updateFreezedAmount(account);
            System.out.println(String.format("Undo prepareMinus account[%s] amount[%f], dtx transaction id: %s.", accountNo, amount, xid));
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}

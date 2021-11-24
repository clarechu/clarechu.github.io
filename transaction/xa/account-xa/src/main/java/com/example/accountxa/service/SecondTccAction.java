package com.example.accountxa.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface SecondTccAction {
    @TwoPhaseBusinessAction(name = "SecondTccAction", commitMethod = "commit", rollbackMethod = "rollback")
    public Boolean prepare(BusinessActionContext businessActionContext, @BusinessActionContextParameter(paramName = "accountNo") String accountNo, @BusinessActionContextParameter(paramName = "amount") double amount);

    public Boolean commit(BusinessActionContext businessActionContext);

    public Boolean rollback(BusinessActionContext businessActionContext);
}

package com.example.accountxa.service;

import io.seata.rm.tcc.api.BusinessActionContext;

public interface FirstTccAction {
    public boolean prepareMinus(BusinessActionContext businessActionContext, final String accountNo, final double amount);

    public boolean commit(BusinessActionContext businessActionContext);

    public Boolean rollback(BusinessActionContext businessActionContext);
}

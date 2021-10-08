package org.springframework.demo.transaction.tuling.service;


/**
 * Created by xsls on 2019/6/17.
 */


public interface PayService {


    void pay(String accountId, double money);

    void updateProductStore(Integer productId);
}

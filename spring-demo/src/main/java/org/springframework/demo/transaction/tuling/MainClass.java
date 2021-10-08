package org.springframework.demo.transaction.tuling;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.transaction.tuling.config.MainConfig;
import org.springframework.demo.transaction.tuling.service.PayService;

/**
 * Created by xsls on 2019/6/17.
 */
public class MainClass {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);

        PayService payService = (PayService) context.getBean("payServiceImpl");
        payService.pay("123456789",10);

    }
}

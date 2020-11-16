package org.springframework.demo.aop.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.demo.aop.service.AccountService;
import org.springframework.demo.aop.util.TransactionManager;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Jie Zhao
 * @date 2020/11/16 21:06
 */
@Component
public class ProxyServiceFactory {

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransactionManager transactionManager;

	@Bean("proxyAccountService")
	public AccountService getProxyAccountService() {
		AccountService proxyAccountService = (AccountService) Proxy.newProxyInstance(accountService.getClass().getClassLoader(),
				accountService.getClass().getInterfaces(),
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object returnValue = null;
						try {
							// 开启事务
							transactionManager.begin();
							returnValue = method.invoke(accountService, args);
							// 提交事务
							transactionManager.commit();
						} catch (Exception e) {
							// 回滚事务
							transactionManager.rollback();
						} finally {
							// 关闭事务
							transactionManager.close();
						}

						return returnValue;
					}
				});
		return proxyAccountService;
	}
}

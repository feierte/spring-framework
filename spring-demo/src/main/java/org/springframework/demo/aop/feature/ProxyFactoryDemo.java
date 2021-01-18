package org.springframework.demo.aop.feature;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.demo.aop.feature.interceptor.EchoServiceMethodInterceptor;
import org.springframework.demo.aop.feature.targetclass.DefaultEchoService;
import org.springframework.demo.aop.feature.targetclass.EchoService;

/**
 * @author Jie Zhao
 * @date 2021/1/16 15:29
 */
public class ProxyFactoryDemo {

	public static void main(String[] args) {
		DefaultEchoService defaultEchoService = new DefaultEchoService();
		// 注入目标对象（被代理）
		ProxyFactory proxyFactory = new ProxyFactory(defaultEchoService);
		// proxyFactory.setTargetClass(DefaultEchoService.class);
		// 添加 Advice 实现 MethodInterceptor < Interceptor < Advice
		proxyFactory.addAdvice(new EchoServiceMethodInterceptor());
		// 获取代理对象
		EchoService echoService = (EchoService) proxyFactory.getProxy();
		System.out.println(echoService.echo("Hello,World"));
	}
}

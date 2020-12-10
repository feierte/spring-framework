package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Jie Zhao
 * @date 2020/12/9 21:22
 */
public class DefaultUserFactory implements UserFactory, InitializingBean, DisposableBean {

	@PostConstruct
	public void init() {
		System.out.println("@PostConstruct  UserFactory初始化中。。。");
	}

	public void initUserFactory() {
		System.out.println("自定义初始化方法 initUserFactory() : UserFactory 初始化中...");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBean#afterPropertiesSet() : UserFactory 初始化中...");
	}

	@PreDestroy
	public void preDestroy() {
		System.out.println("@PreDestroy : UserFactory 销毁中...");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("DisposableBean#destroy() : UserFactory 销毁中...");
	}

	public void doDestroy() {
		System.out.println("自定义销毁方法 doDestroy() : UserFactory 销毁中...");
	}

	@Override
	public void finalize() throws Throwable {
		System.out.println("当前 DefaultUserFactory 对象正在被垃圾回收...");
	}
}

package org.springframework.demo.ioc.beanfactory;

import javax.annotation.PostConstruct;

/**
 * @author Jie Zhao
 * @date 2020/12/9 21:22
 */
public class DefaultUserFactory implements UserFactory {

	@PostConstruct
	public void init() {
		System.out.println("@PostConstruct  UserFactory初始化中。。。");
	}
}

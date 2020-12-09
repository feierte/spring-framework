package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Jie Zhao
 * @date 2020/12/9 21:27
 */
public class UserFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		return User.createUser();
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}
}

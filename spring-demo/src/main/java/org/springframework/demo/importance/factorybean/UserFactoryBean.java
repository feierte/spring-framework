package org.springframework.demo.importance.factorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.demo.aop.quickstart.domain.User;

/**
 * @author jie zhao
 * @date 2022/11/12 20:55
 */
public class UserFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		User user = new User();
		user.setUsername("张三");
		user.setId("永远十八岁");
		return user;
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}
}

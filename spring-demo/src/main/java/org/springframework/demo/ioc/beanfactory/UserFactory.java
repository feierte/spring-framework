package org.springframework.demo.ioc.beanfactory;

/**
 * @author Jie Zhao
 * @date 2020/12/9 21:21
 */
public interface UserFactory {

	default User createUser() {
		return User.createUser();
	}
}

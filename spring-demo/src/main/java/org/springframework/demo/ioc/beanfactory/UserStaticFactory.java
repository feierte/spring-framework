package org.springframework.demo.ioc.beanfactory;

/**
 * 用于测试 <bean/> 标签的 factory-method 属性，用于实例化对象
 *
 * @author Jie Zhao
 * @date 2024/7/30 22:16
 */
public class UserStaticFactory {

	public static User getUser() {
		return User.createUser();
	}
}

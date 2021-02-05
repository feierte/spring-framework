package org.springframework.demo.environment;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.demo.ioc.beanfactory.User;

/**
 * @author Jie Zhao
 * @date 2021/2/5 15:53
 */
public class PropertyPlaceholderConfigurerDemo {

	public static void main(String[] args) {

		// 创建并且启动 Spring 应用上下文
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("META-INF/environment/placeholders-resolver.xml");

		User user = context.getBean("user", User.class);
		System.out.println(user);

		// 关闭 Spring 应用上下文
		context.close();
	}
}

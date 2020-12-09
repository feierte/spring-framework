package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Jie Zhao
 * @date 2020/12/9 20:34
 */
public class BeanAliasDemo {

	public static void main(String[] args) {
		String location = "classpath:/META-INF/ioc/beanfactory/bean-definitions-context.xml";
		BeanFactory beanFactory = new ClassPathXmlApplicationContext(location);

		// 通过别名 获取bean
		User xiaomage = beanFactory.getBean("xiaomage-user", User.class);
		User user = beanFactory.getBean("user", User.class);
		System.out.println(xiaomage == user);
	}
}

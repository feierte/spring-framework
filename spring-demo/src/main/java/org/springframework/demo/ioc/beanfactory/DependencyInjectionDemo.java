package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Jie Zhao
 * @date 2020/12/5 11:44
 */
public class DependencyInjectionDemo {

	public static void main(String[] args) {

		String location = "classpath:/META-INF/ioc/beanfactory/dependency-injection-context.xml";
		BeanFactory beanFactory = new ClassPathXmlApplicationContext(location);
		UserRepository users = beanFactory.getBean("userRepository", UserRepository.class);
		System.out.println(users);
		System.out.println(users.getBeanFactory());
		System.out.println(users.getBeanFactory() == beanFactory);
		System.out.println(users.getObjectFactory().getObject() == beanFactory);

	}
}

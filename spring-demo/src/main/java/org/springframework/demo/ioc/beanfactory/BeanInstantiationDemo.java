package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Jie Zhao
 * @date 2020/12/9 21:17
 */
public class BeanInstantiationDemo {

	public static void main(String[] args) throws Exception {
		String location = "classpath:/META-INF/ioc/beanfactory/bean-instantiation-context.xml";
		BeanFactory beanFactory = new ClassPathXmlApplicationContext(location);
		User user = beanFactory.getBean("user-by-static-method", User.class);
		User user2 = beanFactory.getBean("user-by-instance-method", User.class);
		System.out.println(user);
		System.out.println(user2);
		System.out.println(user == user2);

		// 通过FactoryBean实例化bean
		/*UserFactoryBean userFactoryBean = beanFactory.getBean("user-by-FactoryBean", UserFactoryBean.class);
		User user3 = userFactoryBean.getObject();*/
		User user3 = beanFactory.getBean("user-by-FactoryBean", User.class);
		System.out.println(user3);
	}
}

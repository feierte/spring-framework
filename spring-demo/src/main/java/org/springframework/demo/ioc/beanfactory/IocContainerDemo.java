package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * @author Jie Zhao
 * @date 2020/12/5 14:29
 */
public class IocContainerDemo {

	public static void main(String[] args) {
		// 创建IOC容器
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 加载配置
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions("classpath:/META-INF/ioc/beanfactory/dependency-injection-context.xml");

		// 获取bean
		UserRepository users = beanFactory.getBean("userRepository", UserRepository.class);
		System.out.println(users);
	}
}

package org.springframework.demo.ioc.beanfactory.dependency.di;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.demo.ioc.beanfactory.UserHolder;

/**
 * @author Jie Zhao
 * @date 2020/12/17 20:04
 */
public class XmlDependencySetterInjectionDemo {

	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		String location = "classpath:/META-INF/ioc/beanfactory/di/dependency-setter-injection.xml";
		beanDefinitionReader.loadBeanDefinitions(location);
		UserHolder userHolder = beanFactory.getBean(UserHolder.class);
		System.out.println(userHolder);

	}
}

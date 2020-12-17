package org.springframework.demo.ioc.beanfactory.di;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.demo.ioc.beanfactory.UserHolder;

/**
 * @author Jie Zhao
 * @date 2020/12/17 20:29
 */
public class AutowiringByNameDependencySetterInjectionDemo {

	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		String location = "classpath:/META-INF/ioc/beanfactory/di/autowiring-dependency-setter-injection.xml";
		beanDefinitionReader.loadBeanDefinitions(location);
		UserHolder userHolder = beanFactory.getBean(UserHolder.class);
		System.out.println(userHolder);
	}
}

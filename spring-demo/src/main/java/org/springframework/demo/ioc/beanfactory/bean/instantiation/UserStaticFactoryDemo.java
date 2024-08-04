package org.springframework.demo.ioc.beanfactory.bean.instantiation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.demo.ioc.beanfactory.UserStaticFactory;

/**
 * 测试 <bean/> 标签的 factory-method 属性
 * @author Jie Zhao
 * @date 2024/7/30 22:20
 */
public class UserStaticFactoryDemo {

	public static void main(String[] args) {
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("org/springframework/demo/ioc/beanfactory/bean/instantiation/applicationContext.xml"));
		UserStaticFactory userStaticFactory = (UserStaticFactory) beanFactory.getBean("userStaticFactory");
		System.out.println(userStaticFactory);
	}
}

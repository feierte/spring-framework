package org.springframework.demo.ioc.beanfactory.dependency.di;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.demo.ioc.beanfactory.User;
import org.springframework.demo.ioc.beanfactory.UserHolder;

/**
 * @author Jie Zhao
 * @date 2020/12/17 20:04
 */
public class AnnotationDependencySetterInjectionDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(applicationContext);

		String location = "classpath:/META-INF/ioc/beanfactory/di/dependency-setter-injection.xml";
		beanDefinitionReader.loadBeanDefinitions(location);

		// 注册配置类（@Configuration）
		applicationContext.register(AnnotationDependencySetterInjectionDemo.class);
		applicationContext.refresh();

		UserHolder userHolder = applicationContext.getBean(UserHolder.class);
		System.out.println(userHolder);
		applicationContext.close();
	}

	@Bean
	public UserHolder userHolder(User user) {
		UserHolder userHolder = new UserHolder();
		userHolder.setUser(user);
		return userHolder;
	}
}

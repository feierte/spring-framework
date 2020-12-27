package org.springframework.demo.ioc.beanfactory.dependency.di;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.ioc.beanfactory.UserHolder;

/**
 * @author Jie Zhao
 * @date 2020/12/17 20:04
 */
public class ApiDependencySetterInjectionDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(applicationContext);

		String location = "classpath:/META-INF/ioc/beanfactory/di/dependency-setter-injection.xml";
		beanDefinitionReader.loadBeanDefinitions(location);

		// 注册UserHolder 的 BeanDefinition
		applicationContext.registerBeanDefinition("userHolder", createUserHolderBeanDefinition());
		applicationContext.refresh();

		UserHolder userHolder = applicationContext.getBean(UserHolder.class);
		System.out.println(userHolder);
		applicationContext.close();
	}


	private static BeanDefinition createUserHolderBeanDefinition() {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(UserHolder.class);
		beanDefinitionBuilder.addPropertyReference("user","superUser");
		return beanDefinitionBuilder.getBeanDefinition();
	}

	/*@Bean
	public UserHolder userHolder(User user) {
		UserHolder userHolder = new UserHolder();
		userHolder.setUser(user);
		return userHolder;
	}*/
}

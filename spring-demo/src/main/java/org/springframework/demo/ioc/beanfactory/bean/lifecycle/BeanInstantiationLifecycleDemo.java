package org.springframework.demo.ioc.beanfactory.bean.lifecycle;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.demo.ioc.beanfactory.User;
import org.springframework.demo.ioc.beanfactory.UserHolder;

/**
 * @author Jie Zhao
 * @date 2020/12/30 19:34
 */
public class BeanInstantiationLifecycleDemo {

	public static void main(String[] args) {
		executeBeanFactory();

		System.out.println("--------------------------------");

		executeApplicationContext();
	}

	private static void executeBeanFactory() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 方法一：添加 BeanPostProcessor 实现 MyInstantiationAwareBeanPostProcessor
		// beanFactory.addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());
		// 方法二：将 MyInstantiationAwareBeanPostProcessor 作为 Bean 注册
		// 基于 XML 资源 BeanDefinitionReader 实现
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		String[] locations = {"META-INF/ioc/beanfactory/dependency-lookup-context.xml",
				"META-INF/ioc/beanfactory/bean/lifecycle/bean-constructor-dependency-injection.xml"};
		int beanNumbers = beanDefinitionReader.loadBeanDefinitions(locations);
		System.out.println("已加载 BeanDefinition 数量：" + beanNumbers);
		// 通过 Bean Id 和类型进行依赖查找
		User user = beanFactory.getBean("user", User.class);
		System.out.println(user);

		User superUser = beanFactory.getBean("superUser", User.class);
		System.out.println(superUser);

		// 构造器注入按照类型注入，resolveDependency
		UserHolder userHolder = beanFactory.getBean("userHolder", UserHolder.class);
		System.out.println(userHolder);
	}

	private static void executeApplicationContext() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		String[] locations = {"META-INF/ioc/beanfactory/dependency-lookup-context.xml",
				"META-INF/ioc/beanfactory/bean/lifecycle/bean-constructor-dependency-injection.xml"};
		applicationContext.setConfigLocations(locations);
		// 启动应用上下文
		applicationContext.refresh();

		User user = applicationContext.getBean("user", User.class);
		System.out.println(user);

		User superUser = applicationContext.getBean("superUser", User.class);
		System.out.println(superUser);

		// 构造器注入按照类型注入，resolveDependency
		UserHolder userHolder = applicationContext.getBean("userHolder", UserHolder.class);
		System.out.println(userHolder);

		// 关闭应用上下文
		applicationContext.close();

	}
}


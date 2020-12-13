package org.springframework.demo.ioc.beanfactory.lookup;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.demo.ioc.beanfactory.User;

/**
 * @author Jie Zhao
 * @date 2020/12/12 22:45
 */
public class ObjectProviderDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext();

		applicationContext.register(ObjectProviderDemo.class);
		// 启动应用上下文
		applicationContext.refresh();

		lookupByObjectProvider(applicationContext);
		lookupIfAvailable(applicationContext);
		lookupByStreamOps(applicationContext);
		// 关闭应用上下文
		applicationContext.close();
	}

	private static void lookupByStreamOps(AnnotationConfigApplicationContext applicationContext) {
		ObjectProvider<String> objectProvider = applicationContext.getBeanProvider(String.class);
//        Iterable<String> stringIterable = objectProvider;
//        for (String string : stringIterable) {
//            System.out.println(string);
//        }
		// Stream -> Method reference
		objectProvider.stream().forEach(System.out::println);
	}

	private static void lookupIfAvailable(AnnotationConfigApplicationContext applicationContext) {
		ObjectProvider<User> userObjectProvider = applicationContext.getBeanProvider(User.class);
		User user = userObjectProvider.getIfAvailable(User::createUser);
		System.out.println("当前 User 对象：" + user);
	}

	@Bean
	@Primary
	public String helloWorld() { // 方法名就是 Bean 名称 = "helloWorld"
		return "Hello,World";
	}

	@Bean
	public String message() {
		return "Message";
	}

	private static void lookupByObjectProvider(ApplicationContext applicationContext) {
		ObjectProvider<String> beanProvider = applicationContext.getBeanProvider(String.class);
		String object = beanProvider.getObject();
		System.out.println(object);
	}
}

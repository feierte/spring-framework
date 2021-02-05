package org.springframework.demo.ioc.annotation.config.enable;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.ioc.annotation.config.ComponentScanConfiguration;

/**
 * @author Jie Zhao
 * @date 2021/2/5 11:16
 */
@EnableHelloWorld
public class EnableModuleDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(EnableModuleDemo.class);
		applicationContext.refresh();

		String helloWorld = applicationContext.getBean("helloWorld", String.class);
		System.out.println(helloWorld);
		applicationContext.close();
	}
}

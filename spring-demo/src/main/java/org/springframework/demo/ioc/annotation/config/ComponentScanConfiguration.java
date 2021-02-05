package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Jie Zhao
 * @date 2021/2/5 9:12
 */
@ComponentScan(basePackages = "org.springframework.demo.ioc.annotation")
public class ComponentScanConfiguration {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(ComponentScanConfiguration.class);
		applicationContext.refresh();
		applicationContext.close();
	}
}

package org.springframework.demo.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApplicationContextDemo {

	private static char c;
	private static boolean flag;

	public static void main(String[] args) {
		System.out.println(flag);
		System.out.println(c);
		System.out.println('\u0000');
		System.out.println('\u0001');
		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		AppConfig appConfig = context.getBean(AppConfig.class);
		System.out.println(appConfig);
	}
}

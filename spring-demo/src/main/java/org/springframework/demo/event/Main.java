package org.springframework.demo.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jie Zhao
 * @date 2021/9/29 11:42
 */
@Configuration
@ComponentScan(basePackages = {"org.springframework.demo.event"})
public class Main {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Main.class);

		applicationContext.publishEvent(new MyApplicationEvent("自定义事件"));

		applicationContext.close();

	}
}

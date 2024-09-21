package org.springframework.demo.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.demo.transaction.heima.event.MyApplicationEvent;

/**
 * @author Jie Zhao
 * @date 2021/9/29 11:42
 */
public class Main {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringEventConfiguration.class);

		applicationContext.publishEvent(new LoginEvent("Login", "小王"));

		applicationContext.close();

	}
}

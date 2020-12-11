package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.demo.ioc.annotation.event.EventListener;
import org.springframework.demo.ioc.annotation.event.EventSource;

/**
 * @author Jie Zhao
 * @date 2020/11/2 22:07
 * <p>
 * @see DependsOn
 */
@Configuration
public class DependsOnConfiguration {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext();
		applicationContext.register(DependsOnConfiguration.class, EventListener.class, EventSource.class);
		applicationContext.refresh();
		applicationContext.start();
		applicationContext.close();
	}
}

package org.springframework.demo.ioc.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;

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
				new AnnotationConfigApplicationContext("org.springframework.demo");
		applicationContext.start();
	}
}

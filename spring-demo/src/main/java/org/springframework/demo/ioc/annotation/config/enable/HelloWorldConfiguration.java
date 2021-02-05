package org.springframework.demo.ioc.annotation.config.enable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jie Zhao
 * @date 2021/2/5 11:14
 */
@Configuration
public class HelloWorldConfiguration {

	@Bean
	public String helloWorld() {
		return "hello world!";
	}
}

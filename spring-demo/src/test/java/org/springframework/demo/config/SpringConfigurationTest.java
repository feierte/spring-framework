package org.springframework.demo.config;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.service.UserService;

/**
 * @author Jie Zhao
 * @date 2020/10/27 20:11
 */
public class SpringConfigurationTest {

	@Test
	public void testComponentScan() {

		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo");
		UserService userService = applicationContext.getBean("userService", UserService.class);
		userService.saveUser();
	}
}

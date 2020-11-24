package org.springframework.demo.aop;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.aop.config.AopConfig;
import org.springframework.demo.aop.domain.User;
import org.springframework.demo.aop.service.UserService;
import org.springframework.demo.ioc.annotation.config.JdbcConfig;
import org.springframework.demo.ioc.annotation.config.SpringConfiguration;
import org.springframework.demo.ioc.annotation.config.YamlPropertySourceFactoryDemo;

/**
 * @author Jie Zhao
 * @date 2020/11/24 20:44
 */

public class AopTest {

	@Test
	public void aopTest() {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(AopConfig.class, YamlPropertySourceFactoryDemo.class);
		UserService userService = applicationContext.getBean("userService1", UserService.class);
		User user = new User();
		user.setId("1");
		user.setUsername("test");
		userService.saveUser(user);
	}

}

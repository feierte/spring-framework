package org.springframework.demo.ioc.annotation.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.ioc.annotation.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * @author Jie Zhao
 * @date 2020/10/27 20:11
 */
public class SpringConfigurationTest {

	@Test
	public void testComponentScan() {

		ApplicationContext applicationContext = new AnnotationConfigApplicationContext("org.springframework.demo.ioc.annotation.service");
		// ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
		// ApplicationContext applicationContext = new AnnotationConfigApplicationContext("org.springframework.demo");
		UserService userService = applicationContext.getBean("userService", UserService.class);
		userService.saveUser();
	}

	@Test
	public void testBean() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(SpringConfiguration.class);
		JdbcTemplate jdbcTemplate = applicationContext.getBean("jdbcTemplate", JdbcTemplate.class);
		System.out.println(jdbcTemplate.getDataSource());
	}

	@Test
	public void testImport() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo.ioc.annotation");
		DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
		System.out.println(dataSource);
		Arrays.stream(applicationContext.getBeanDefinitionNames()).forEach(System.out::println);
	}

	@Test
	public void testImportSelector() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo.ioc");
		UserService userService =
				applicationContext.getBean("org.springframework.demo.ioc.annotation.service.impl.UserServiceImpl", UserService.class);
		userService.saveUser();
	}
}

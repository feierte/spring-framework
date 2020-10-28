package org.springframework.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

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

	@Test
	public void testBean() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo");
		JdbcTemplate jdbcTemplate = applicationContext.getBean("jdbcTemplate", JdbcTemplate.class);
		System.out.println(jdbcTemplate.getDataSource());
	}

	@Test
	public void testImport() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo");
		DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
		System.out.println(dataSource);
	}
}

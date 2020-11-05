package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Jie Zhao
 * @date 2020/10/27 20:10
 */
@Configuration
@ComponentScan("org.springframework.demo.ioc.annotation.service")
//@Import(JdbcConfig.class)
public class SpringConfiguration {

	/*@Resource
	private DataSource dataSource;

	@Bean(name = "dataSource", autowireCandidate = false)
	public DataSource dataSource() {
		return new DriverManagerDataSource();
	}

	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}*/
}

package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.demo.ioc.annotation.config.conditional.ConditionalConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author Jie Zhao
 * @date 2020/10/27 20:10
 */
@Configuration
@ComponentScan("org.springframework.demo.ioc.annotation.service")
@Import({JdbcConfig.class, ConditionalConfiguration.class})
public class SpringConfiguration {

	@Resource
	private DataSource dataSource;

	/*@Bean(name = "dataSource", autowireCandidate = false)
	public DataSource dataSource() {
		return new DriverManagerDataSource();
	}*/

	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}
}

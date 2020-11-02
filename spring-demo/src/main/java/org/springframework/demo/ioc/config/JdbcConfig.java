package org.springframework.demo.ioc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author Jie Zhao
 * @date 2020/10/28 21:47
 */
//@Configuration
public class JdbcConfig {

	@Bean
	public DataSource dataSource(){
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		return dataSource;
	}
}

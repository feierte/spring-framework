package org.springframework.demo.ioc.annotation.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author Jie Zhao
 * @date 2020/10/28 21:47
 */
@Configuration
@PropertySource("classpath:db.properties")
public class JdbcConfig {


	@Value("${jdbc.driver}")
	private String driver;

	@Value("${jdbc.url}")
	private String url;

	@Value("${jdbc.username}")
	private String username;

	@Value("${jdbc.password}")
	private String password;

	@Bean
	@Primary
	public DataSource dataSource(){
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(driver);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		System.out.println("driver: " + driver);
		return dataSource;
	}


	/*@Bean
	public DataSource dataSource(){
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		return dataSource;
	}*/

	@Bean("connection")
	public Connection getConnection(DataSource dataSource) {
		// 1、初始化事务同步管理器
		TransactionSynchronizationManager.initSynchronization();
		// 2、使用Spring的数据源工具类获取当前线程的连接
		Connection connection = DataSourceUtils.getConnection(dataSource);
		return connection;
	}
}


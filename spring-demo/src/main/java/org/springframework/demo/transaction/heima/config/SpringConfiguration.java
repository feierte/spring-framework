package org.springframework.demo.transaction.heima.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Jie Zhao
 * @date 2021/10/7 22:41
 */
@Configuration
@Import({JdbcConfig.class, TransactionManagerConfig.class})
@ComponentScan(basePackages = {"org.springframework.demo.transaction.heima"})
@PropertySource("classpath:db.properties")
@EnableTransactionManagement
public class SpringConfiguration {
}

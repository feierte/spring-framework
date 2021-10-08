package org.springframework.demo.transaction.heima.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Jie Zhao
 * @date 2021/10/7 22:41
 */
@Configuration
@Import(JdbcConfig.class)
@PropertySource("classpath:db.properties")
public class SpringConfiguration {
}

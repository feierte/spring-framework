package org.springframework.demo.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * Spring的配置类，替代了applicationContext.xml配置文件
 * @author Jie Zhao
 * @date 2020/12/1 20:22
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.demo.mvc",
		excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class))
public class SpringConfiguration {
}

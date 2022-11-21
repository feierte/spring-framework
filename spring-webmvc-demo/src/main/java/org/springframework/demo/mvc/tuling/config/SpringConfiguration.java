package org.springframework.demo.mvc.tuling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.demo.mvc.tuling.controller.HelloController;
import org.springframework.stereotype.Controller;

/**
 * Spring的配置类，替代了applicationContext.xml配置文件
 * @author Jie Zhao
 * @date 2020/12/1 20:22
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.demo.mvc.tuling",
		excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class))
public class SpringConfiguration {

}

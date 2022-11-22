package org.springframework.demo.mvc.shangguigu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Spring MVC配置类，用于替代springmvc.xml配置文件
 * @author Jie Zhao
 * @date 2020/12/1 20:27
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.demo.mvc",
		includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
useDefaultFilters = false)
public class SpringMvcConfiguration {


}

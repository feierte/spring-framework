package org.springframework.demo.mvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Spring MVC配置类，用于替代springmvc.xml配置文件
 * @author Jie Zhao
 * @date 2020/12/1 20:27
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.demo.mvc")
public class SpringMvcConfiguration {

	/**
	 * 创建视图解析器，并放入IOC容器中
	 * @return
	 */
	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/pages");
		viewResolver.setSuffix(".jsp");
		return viewResolver;
	}
}

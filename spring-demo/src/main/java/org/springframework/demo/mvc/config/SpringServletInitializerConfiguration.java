package org.springframework.demo.mvc.config;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 初始化spring 和 springmvc ioc容器的配置类
 * @author Jie Zhao
 * @date 2020/12/1 20:31
 */
public class SpringServletInitializerConfiguration extends AbstractDispatcherServletInitializer {
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		// 触发父类的onStartup方法
		super.onStartup(servletContext);
		/*
		 * 注册字符集过滤器
		 */
		// 创建字符集过滤器对象
		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
		// 设置使用的字符集
		encodingFilter.setEncoding("UTF-8");
		// 添加到容器中（不是IOC容器，而是ServletContainer）
		servletContext.addFilter("encodingFilter", encodingFilter);
	}

	/**
	 * 用于创建SpringMVC的 IOC容器
	 * @return
	 */
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(SpringMvcConfiguration.class);
		return applicationContext;
	}

	/**
	 * 用于指定DispatcherServlet的请求映射
	 * @return
	 */
	@Override
	protected String[] getServletMappings() {
		return new String[] {"/"};
	}

	/**
	 * 用于创建Spring的 IOC容器
	 * @return
	 */
	@Override
	protected WebApplicationContext createRootApplicationContext() {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(SpringConfiguration.class);
		return applicationContext;
	}
}

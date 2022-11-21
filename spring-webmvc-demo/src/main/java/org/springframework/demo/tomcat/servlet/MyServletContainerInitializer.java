package org.springframework.demo.tomcat.servlet;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import java.util.Set;

/**
 * @author Jie Zhao
 * @date 2021/11/1 11:03
 */
@HandlesTypes({})
public class MyServletContainerInitializer implements ServletContainerInitializer {
	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		System.out.println("MyServletContainerInitializer executed...");
		ServletRegistration.Dynamic dynamic = ctx.addServlet("servletContainerInitializerServlet", "org.springframework.demo.tomcat.servlet.ServletContainerInitializerServlet");
		dynamic.addMapping("/servletContainerInitializerServlet");
	}
}

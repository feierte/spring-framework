package org.springframework.demo.mvc.shangguigu.config;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * @author Jie Zhao
 * @date 2022/11/21 22:02
 */
public class MyWebServletContainerInitializer /*implements WebApplicationInitializer*/ {

	/*@Override
	public void onStartup(ServletContext container) {
		AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();


		ServletRegistration.Dynamic dispatcher =
				container.addServlet("dispatcher", new DispatcherServlet(appContext));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/");
	}*/
}

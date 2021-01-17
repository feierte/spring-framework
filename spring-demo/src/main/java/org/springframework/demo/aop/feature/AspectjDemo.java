package org.springframework.demo.aop.feature;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author Jie Zhao
 * @date 2021/1/16 14:52
 */
@Aspect // 声明为切面
@Configuration
@EnableAspectJAutoProxy
public class AspectjDemo {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.register(AspectjDemo.class);
		applicationContext.refresh();

		AspectjDemo bean = applicationContext.getBean(AspectjDemo.class);
		System.out.println(bean);
		applicationContext.close();
	}
}

package org.springframework.demo.aop.feature;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.demo.aop.feature.aspect.AspectConfiguration;
import org.springframework.demo.aop.feature.aspect.AspectConfiguration2;

/**
 * @author Jie Zhao
 * @date 2021/1/19 19:40
 */
@Configuration // Configuration class
@EnableAspectJAutoProxy // 激活 Aspect 注解自动代理
public class AspectJAnnotatedPointcutDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AspectJAnnotatedPointcutDemo.class,
				AspectConfiguration.class,
				AspectConfiguration2.class);
		context.refresh();

		AspectJAnnotatedPointcutDemo aspectJAnnotationDemo = context.getBean(AspectJAnnotatedPointcutDemo.class);

		aspectJAnnotationDemo.execute();

		context.close();
	}

	public void execute() {
		System.out.println("execute()...");
	}
}
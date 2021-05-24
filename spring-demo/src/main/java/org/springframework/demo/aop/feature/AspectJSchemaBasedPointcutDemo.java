package org.springframework.demo.aop.feature;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.demo.aop.feature.target.EchoService;

/**
 * @author Jie Zhao
 * @date 2021/1/19 19:44
 */
public class AspectJSchemaBasedPointcutDemo {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("classpath:/META-INF/aop/feature/spring-aop-context.xml");
		context.refresh();
		EchoService echoService = context.getBean("echoService", EchoService.class);
		System.out.println(echoService.echo("Hello,World"));
		context.close();
	}
}

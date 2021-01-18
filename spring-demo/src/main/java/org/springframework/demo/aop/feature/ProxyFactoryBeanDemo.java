package org.springframework.demo.aop.feature;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.demo.aop.feature.targetclass.EchoService;

/**
 * @author Jie Zhao
 * @date 2021/1/16 15:21
 */
@Aspect        // 声明为 Aspect 切面
@Configuration // Configuration class
public class ProxyFactoryBeanDemo {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/META-INF/aop/feature/spring-aop-context.xml");

		EchoService echoService = context.getBean("echoServiceProxyFactoryBean", EchoService.class);

		System.out.println(echoService.echo("Hello,World"));

		context.close();
	}
}
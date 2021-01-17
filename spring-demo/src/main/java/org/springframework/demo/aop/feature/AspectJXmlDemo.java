package org.springframework.demo.aop.feature;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Jie Zhao
 * @date 2021/1/16 14:59
 */
@Aspect        // 声明为 Aspect 切面
@Configuration // Configuration class
public class AspectJXmlDemo {

	public static void main(String[] args) {

		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("classpath:/META-INF/aop/feature/spring-aop-context.xml");

//        AspectJXmlDemo aspectJAnnotationDemo = context.getBean(AspectJXmlDemo.class);

		context.close();

	}
}

package org.springframework.demo.importance.factorybean;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.aop.quickstart.domain.User;

/**
 * @author jie zhao
 * @date 2022/11/12 20:58
 */
public class MainTest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(UserFactoryBean.class);

		User user = applicationContext.getBean(User.class);
		System.out.println(user);
	}
}

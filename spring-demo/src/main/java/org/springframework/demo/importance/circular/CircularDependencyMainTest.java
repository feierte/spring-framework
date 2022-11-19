package org.springframework.demo.importance.circular;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author jie zhao
 * @date 2022/11/19 15:46
 */
public class CircularDependencyMainTest {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.scan("org.springframework.demo.importance.circular");
		applicationContext.refresh();
	}
}

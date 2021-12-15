package org.springframework.demo.lifecycle;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Jie Zhao
 * @date 2021/12/14 20:21
 */
public class LifecycleMain {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(HelloLifeCycle.class);
		applicationContext.refresh();


		applicationContext.start();
		applicationContext.close();
	}
}

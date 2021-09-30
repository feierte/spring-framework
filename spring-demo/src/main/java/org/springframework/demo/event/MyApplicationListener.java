package org.springframework.demo.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2021/9/29 11:40
 */
@Component
public class MyApplicationListener /*implements ApplicationListener<MyApplicationEvent>*/ {


	@EventListener
	// @Override
	public void onApplicationEvent(MyApplicationEvent event) {
		String message = event.getMessage();
		System.out.println(message);
	}
}

package org.springframework.demo.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2024/9/21 9:04
 */
@Component
public class AsyncApplicationListener implements ApplicationListener<LoginEvent> {


	@EventListener(condition = "#event.userName.equals('小王')") // 表示该方法是事件监听器，如果有相应事件方式，该方法会被执行
																// @EventListener 还提供条件判定功能，支持 SpringEL 表达式
	@Async // 该事件会异步执行
	public void handleLogin(LoginEvent event) {
		System.out.println("@EventListener：发生用户登陆事件...");
	}

	/**
	 * 非注解实现的事件监听器，需要实现 ApplicationListener 接口
	 * @param event the event to respond to
	 */
	@Override
	public void onApplicationEvent(LoginEvent event) {
		System.out.println("ApplicationListener：发生用户登陆事件...");
	}
}

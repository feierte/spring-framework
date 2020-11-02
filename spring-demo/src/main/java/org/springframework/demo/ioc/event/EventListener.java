package org.springframework.demo.ioc.event;

import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2020/11/2 22:11
 */
@Component
public class EventListener {

	public EventListener() {
		System.out.println("事件监听器对象创建");
	}
}

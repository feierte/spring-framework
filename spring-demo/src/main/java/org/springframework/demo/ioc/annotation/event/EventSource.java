package org.springframework.demo.ioc.annotation.event;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2020/11/2 22:10
 */
@Component
@DependsOn("eventListener") // Spring容器创建eventSource bean之前，要先创建eventListener bean，
public class EventSource {

	public EventSource() {
		System.out.println("事件源对象创建");
	}
}

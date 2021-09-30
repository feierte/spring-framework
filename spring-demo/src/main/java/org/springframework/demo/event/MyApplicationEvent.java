package org.springframework.demo.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2021/9/29 11:40
 */
public class MyApplicationEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private String message;

	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source the object on which the event initially occurred or with
	 *               which the event is associated (never {@code null})
	 */
	public MyApplicationEvent(Object source) {
		super(source);
	}


	public String getMessage() {
		return "自定义事件发生了...";
	}
}

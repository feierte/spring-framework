package org.springframework.demo.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jie Zhao
 * @date 2024/9/21 9:05
 */
public class LoginEvent extends ApplicationEvent {

	private String userName;

	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source the object on which the event initially occurred or with
	 *               which the event is associated (never {@code null})
	 */
	public LoginEvent(Object source, String userName) {
		super(source);
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}
}

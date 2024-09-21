package org.springframework.demo.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jie Zhao
 * @date 2024/9/21 13:57
 */
public class GenericEvent<T> extends ApplicationEvent {

	private T origin;

	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source the object on which the event initially occurred or with
	 *               which the event is associated (never {@code null})
	 */
	public GenericEvent(Object source, T origin) {
		super(source);
		this.origin = origin;
	}

	public T getOrigin() {
		return origin;
	}
}

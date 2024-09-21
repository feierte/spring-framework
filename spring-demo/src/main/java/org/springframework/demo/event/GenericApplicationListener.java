package org.springframework.demo.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2024/9/21 13:58
 */
@Component
public class GenericApplicationListener {

	@EventListener
	public <T> void handleGenericEvent(GenericEvent<T> event) {
		T origin = event.getOrigin();
		System.out.println("Received a generic event with data: " + origin);
	}
}

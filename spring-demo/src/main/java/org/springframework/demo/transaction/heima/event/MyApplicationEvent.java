package org.springframework.demo.transaction.heima.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author 黑马程序员
 * @Company http://www.itheima.com
 */
public class MyApplicationEvent extends ApplicationEvent {

	private static final long serialVersionUID = -6989924624262238793L;
	private Object source;

	public MyApplicationEvent(Object source) {
		super(source);
		this.source = source;
	}

	@Override
	public Object getSource() {
		return source;
	}
}

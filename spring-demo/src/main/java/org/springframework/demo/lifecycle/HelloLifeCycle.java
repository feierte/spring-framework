package org.springframework.demo.lifecycle;

import org.springframework.context.Lifecycle;

/**
 * @author Jie Zhao
 * @date 2021/12/14 20:20
 */
public class HelloLifeCycle implements Lifecycle {

	private volatile boolean running = false;

	public HelloLifeCycle() {
		System.out.println("HelloLifeCycle 构造方法!!!");
	}


	@Override
	public void start() {
		System.out.println("HelloLifeCycle lifycycle start");
		running = true;

	}
	@Override
	public void stop() {
		System.out.println("HelloLifeCycle lifycycle stop");
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}

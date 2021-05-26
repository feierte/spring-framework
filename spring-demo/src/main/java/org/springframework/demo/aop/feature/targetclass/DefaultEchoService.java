package org.springframework.demo.aop.feature.targetclass;

/**
 * @author Jie Zhao
 * @date 2021/1/16 15:16
 */
public class DefaultEchoService implements EchoService {

	@Override
	public String echo(String message) {
		return "[ECHO] " + message;
	}

}

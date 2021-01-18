package org.springframework.demo.aop.feature.targetclass;

/**
 * @author Jie Zhao
 * @date 2021/1/16 15:16
 */
public interface EchoService {

	String echo(String message) throws NullPointerException;

}

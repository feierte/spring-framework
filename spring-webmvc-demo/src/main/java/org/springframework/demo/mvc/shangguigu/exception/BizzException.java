package org.springframework.demo.mvc.shangguigu.exception;

/**
 * @author jie zhao
 * @date 2022/11/29 22:23
 */
public class BizzException extends RuntimeException {

	private static final long serialVersionUID = -1;

	public BizzException() {
		super();
	}

	public BizzException(String message) {
		super(message);
	}
}

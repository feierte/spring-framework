package org.springframework.demo.mvc.shangguigu.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author jie zhao
 * @date 2022/11/29 22:20
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizzException.class)
	public String bizzExceptionHandle() {
		return null;
	}
}

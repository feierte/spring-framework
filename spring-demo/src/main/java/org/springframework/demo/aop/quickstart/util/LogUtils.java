package org.springframework.demo.aop.quickstart.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2020/11/24 20:37
 */
@Component
@Aspect
public class LogUtils {

	@Before("execution(* org.springframework.demo.aop.quickstart.service.impl.*.saveUser(..))") // 表示当前方法是一个前置通知
	public void printLog() {
		System.out.println("执行打印日志功能");
	}
}

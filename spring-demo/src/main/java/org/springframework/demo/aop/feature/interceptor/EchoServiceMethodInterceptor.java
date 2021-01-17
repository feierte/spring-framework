package org.springframework.demo.aop.feature.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * @author Jie Zhao
 * @date 2021/1/16 15:24
 */
public class EchoServiceMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		System.out.println("拦截 EchoService 的方法：" + method);
		return invocation.proceed();
	}
}

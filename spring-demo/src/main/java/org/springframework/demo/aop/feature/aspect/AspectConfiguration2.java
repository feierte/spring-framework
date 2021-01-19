package org.springframework.demo.aop.feature.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;

/**
 * @author Jie Zhao
 * @date 2021/1/19 19:42
 */
@Aspect
public class AspectConfiguration2 implements Ordered {

	@Before("execution(public * *(..))")          // Join Point 拦截动作
	public void beforeAnyPublicMethod2() {
		System.out.println("@Before any public method.(2)");
	}

	@Override
	public int getOrder() {
		return 0;
	}
}

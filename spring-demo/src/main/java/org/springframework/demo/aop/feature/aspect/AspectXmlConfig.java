package org.springframework.demo.aop.feature.aspect;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Random;

/**
 * @author Jie Zhao
 * @date 2021/1/19 19:47
 */
public class AspectXmlConfig {

	public Object aroundAnyPublicMethod(ProceedingJoinPoint pjp) throws Throwable {
		Random random = new Random();
		/*if (random.nextBoolean()) {
			throw new RuntimeException("For Purpose from XML configuration.");
		}*/
		System.out.println("@Around any public method : " + pjp.getSignature());
		return pjp.proceed();
	}

	public void beforeAnyPublicMethod() {
		System.out.println("@Before any public method.");
	}

	public void finalizeAnyPublicMethod() {
		System.out.println("@After any public method.");
	}

	public void afterAnyPublicMethod() {
		System.out.println("@AfterReturning any public method.");
	}

	public void afterThrowingAnyPublicMethod() {
		System.out.println("@AfterThrowing any public method.");
	}
}

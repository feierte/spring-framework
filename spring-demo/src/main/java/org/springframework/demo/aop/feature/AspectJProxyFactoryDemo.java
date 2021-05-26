package org.springframework.demo.aop.feature;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.demo.aop.feature.aspect.AspectConfiguration;
import org.springframework.demo.aop.feature.targetclass.DefaultEchoService;
import org.springframework.demo.aop.feature.targetclass.EchoService;

/**
 * AspectJProxyFactory示例
 */
public class AspectJProxyFactoryDemo {

	public static void main(String[] args) {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DefaultEchoService());
		// 注意：此处得MyAspect类上面的@Aspect注解必不可少
//		 proxyFactory.addAspect(MyAspect.class);
		proxyFactory.addAspect(AspectConfiguration.class);
		//proxyFactory.setProxyTargetClass(true);//是否需要使用CGLIB代理
		EchoService proxy = proxyFactory.getProxy();
		proxy.echo("hello");

		System.out.println(proxy.getClass()); //class com.sun.proxy.$Proxy6
	}

	@Aspect
	class MyAspect {

		@Pointcut("execution(public * *(..))")
		private void beforeAdd() {
		}

		@Before("beforeAdd()")
		public void before1() {
			System.out.println("-----------before-----------");
		}

	}
}

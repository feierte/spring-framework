/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop;

/**
 * Core Spring pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 *
 * <p>切点：用于定位连接点（Joinpoint）
 * Pointcut由ClassFilter和MethodMatcher构成，它通过ClassFilter定位到某些特定类上，
 * 通过MethodMatcher定位到某些特定方法上，这样Pointcut就拥有了描述某些类
 *
 * <p>Spring 提供了6种类型的切点
 * 1、静态方法切点（StaticMethodMatcherPointcut）：默认情况下它匹配所有的类。它有两个主要的子类：
 * 	NameMatchMethodPointcut:提供简单字符串匹配方法签名
 * 	AbstractRegexpMethodPointcut:使用正则表达式匹配方法签名。
 * 2、动态方法切点（DynamicMethodMatcherPointcut）：是动态方法切点的抽象基类，默认情况下它匹配所有的类。
 * 3、注解切点（AnnotationMatchingPointcut）：支持在Bean中直接通过Java 5.0注解标签定义的切点
 * 4、表达式切点（ExpressionPointcut）：支持AspectJ切点表达式语法而定义的接口。
 * 5、流程切点（ControllFlowPointcut）：这是一种特殊的切点，它根据程序执行堆栈的信息查看目标方法是否由一个方法直接或间接发起调用，
 * 	以此判断是否为匹配的连接点。
 * 6、符合切点（ComposablePointcut）：为创建多个切点而提供的方便操作类。
 */
public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 * @return the ClassFilter (never {@code null})
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 * @return the MethodMatcher (never {@code null})
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * Canonical Pointcut instance that always matches.
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}

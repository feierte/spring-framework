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
 * @apiNote 切点：用于定位连接点（Joinpoint）
 * Pointcut由ClassFilter和MethodMatcher构成，它通过ClassFilter定位到某些特定类上，
 * 通过MethodMatcher定位到某些特定方法上，这样Pointcut就拥有了描述某些类
 *
 * <p>Spring 提供了6种类型的切点
 * 1、静态方法切点（StaticMethodMatcherPointcut）：默认情况下它匹配所有的类。它有两个主要的子类：
 * 	NameMatchMethodPointcut:提供简单字符串匹配方法签名
 * 	AbstractRegexpMethodPointcut:使用正则表达式匹配方法签名。
 * 2、动态方法切点（DynamicMethodMatcherPointcut）：是动态方法切点的抽象基类，默认情况下它匹配所有的类。
 * 3、注解切点（AnnotationMatchingPointcut）：支持在Bean中直接通过Java 5.0注解标签定义的切点
 * 4、表达式切点（ExpressionPointcut）：支持AspectJ切点表达式语法而定义的接口。主要是为了支持AspectJ切点表达式语法而定义的接口。
 *  这个是最强大的，Spring支持11种切点表达式
 * 5、流程切点（ControlFlowPointcut）：这是一种特殊的切点，它根据程序执行堆栈的信息查看目标方法是否由一个方法直接或间接发起调用，
 * 	以此判断是否为匹配的连接点。
 * 6、复合切点（ComposablePointcut）：为创建多个切点而提供的方便操作类。
 *
 * <p>Spring中既有静态切点，又有动态切点。Spring采用这样的机制：在创建代理时对目标类的每个连接点使用静态切点检查，
 * 如果仅通过静态切点检查就可以知道连接点是不匹配的，则在运行时就不在进行动态检查；如果静态切点检查是匹配的，则在运行时才进行动态切点检查。
 *
 * <p>其实动态代理中的“动态”是相对于那些编译期生成代理和类加载期生成代理而言的。动态代理是运行时动态产生的代理。、
 * 在Spring中，不管是静态切面还是动态切面，都是通过动态代理技术实现的。所谓静态切面，是指在生成代理对象时就确定了增强是否需要织入目标类的连接点；
 * 而动态切面是指必须在运行期根据方法入参的值来判断增强是否需要织入目标类的连接点上。
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

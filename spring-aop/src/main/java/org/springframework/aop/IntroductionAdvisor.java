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
 * Superinterface for advisors that perform one or more AOP <b>introductions</b>.
 *
 * <p>This interface cannot be implemented directly; subinterfaces must
 * provide the advice type implementing the introduction.
 *
 * <p>Introduction is the implementation of additional interfaces
 * (not implemented by a target) via AOP advice.
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 *
 * @apiNote 引介切面：是引介增强的封装器，通过引介切面，可以更容易地为现有对象添加任何接口的实现。
 *
 * <p>IntroductionAdvisor和PointcutAdvisor接口不同，它仅有一个类过滤器ClassFilter，而没有MethodMatcher，
 * 这是因为引介切面的切点是类级别的，而Pointcut的切点是方法级别的。
 *
 * <p>IntroductionAdvisor有两个实现类，分别是DefaultIntroductionAdvisor和DeclareParentsAdvisor，
 * 前者是引介切面最常用的实现类，后者用于实现使用AspectJ语言的@DeclareParent注解表示的引介切面。
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * Return the filter determining which target classes this introduction
	 * should apply to.
	 * <p>This represents the class part of a pointcut. Note that method
	 * matching doesn't make sense to introductions.
	 * @return the class filter
	 */
	ClassFilter getClassFilter();

	/**
	 * Can the advised interfaces be implemented by the introduction advice?
	 * Invoked before adding an IntroductionAdvisor.
	 * @throws IllegalArgumentException if the advised interfaces can't be
	 * implemented by the introduction advice
	 */
	void validateInterfaces() throws IllegalArgumentException;

}

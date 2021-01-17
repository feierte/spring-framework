/*
 * Copyright 2002-2007 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Subinterface of AOP Alliance MethodInterceptor that allows additional interfaces
 * to be implemented by the interceptor, and available via a proxy using that
 * interceptor. This is a fundamental AOP concept called <b>introduction</b>.
 *
 * <p>Introductions are often <b>mixins</b>, enabling the building of composite
 * objects that can achieve many of the goals of multiple inheritance in Java.
 *
 * @author Rod Johnson
 * @see DynamicIntroductionAdvice
 *
 * <p>IntroductionInterceptor（引介增强）表示在目标类中添加一些新的方法和属性
 * 引介增强是一种特殊的增强，它不是在目标方法周围织入增强，而是为目标类创建新的方法和属性，所以引介增强的连接点是类级别的，而非方法级别的。
 * <p>通过引介增强，可以为目标类添加一个接口的实现，即原来目标类未实现某个接口，通过引介增强可以为目标类创建实现某接口的代理。
 * Spring定义的引介增强接口IntroductionInterceptor没有任何方法，Spring为该接口提供了DelegatingIntroductionInterceptor实现类。
 * 一般情况下，通过扩展该实现类定义自己的引介增强类。
 *
 */
public interface IntroductionInterceptor extends MethodInterceptor, DynamicIntroductionAdvice {

}

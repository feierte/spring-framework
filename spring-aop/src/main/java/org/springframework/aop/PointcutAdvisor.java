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
 * Superinterface for all Advisors that are driven by a pointcut.
 * This covers nearly all advisors except introduction advisors,
 * for which method-level matching doesn't apply.
 *
 * @author Rod Johnson
 *
 * <p>
 * PointcutAdvisor主要有6个具体的实现类：
 * 	1、DefaultPointcutAdvisor：最常用的切面类型，它可以通过任意Pointcut和Advice定义一个切面，唯一不支持的是引介的切面类型，
 * 		一般可以通过扩展该类实现自定义的切面。
 *  2、NameMatchMethodPointcutAdvisor：通过该类可以定义按方法名定义切点的切面。
 *  3、RegexpMethodPointcutAdvisor：对于按正则表达式匹配方法名进行切点定义的切面，可以通过扩展该实现类进行操作。
 *  4、StaticMethodMatcherPointcutAdvisor：静态方法匹配器切点定义的切面，默认情况下匹配所有的目标类。
 *  5、AspectJExpressionPointcutAdvisor：用于AspectJ切点表达式定义切点的切面。
 *  6、AspectJPointcutAdvisor：用于AspectJ语法定义切点的切面
 * 这些Advisor的实现类都可以在Pointcut中找到对应物，实际上，它们都是通过扩展对应的Pointcut实现类并实现PointcutAdvisor接口进行定义的。
 */
public interface PointcutAdvisor extends Advisor {

	/**
	 * Get the Pointcut that drives this advisor.
	 */
	Pointcut getPointcut();

}

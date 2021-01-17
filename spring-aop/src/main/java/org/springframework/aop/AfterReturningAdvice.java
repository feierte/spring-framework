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

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * After returning advice is invoked only on normal method return, not if an
 * exception is thrown. Such advice can see the return value, but cannot change it.
 *
 * @author Rod Johnson
 * @see MethodBeforeAdvice
 * @see ThrowsAdvice
 *
 * <p>方法后置增强，表示在目标方法执行后实施增强
 */
public interface AfterReturningAdvice extends AfterAdvice {

	/**
	 * Callback after a given method successfully returned.
	 * @param returnValue the value returned by the method, if any 目标实例方法返回的结果
	 * @param method the method being invoked 目标类的方法
	 * @param args the arguments to the method 目标实例方法的入参
	 * @param target the target of the method invocation. May be {@code null}. 目标类实例
	 * @throws Throwable if this object wishes to abort the call.
	 * Any exception thrown will be returned to the caller if it's
	 * allowed by the method signature. Otherwise the exception
	 * will be wrapped as a runtime exception.
	 * 假设在后置增强中抛出异常，如果该异常是目标方法声明的异常，则该异常归并到目标方法中；如果不是目标方法所声明的异常，
	 * 则Spring将其转为运行期异常抛出。
	 */
	void afterReturning(@Nullable Object returnValue, Method method, Object[] args, @Nullable Object target) throws Throwable;

}

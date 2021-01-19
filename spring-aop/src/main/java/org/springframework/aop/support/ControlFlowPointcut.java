/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Pointcut and method matcher for use in simple <b>cflow</b>-style pointcut.
 * Note that evaluating such pointcuts is 10-15 times slower than evaluating
 * normal pointcuts, but they are useful in some cases.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * <p>流程切点（ControlFlowPointcut）：代表由某个方法直接或间接发起调用的其他方法。
 * Spring的流程切面由DefaultPointcutAdvisor和ControlFlowPointcut实现。
 *
 * <pre> {@code
 * 	public class WaiterDelegate {
 * 	    private Waiter waiter;
 * 	    public void service (String clientName) { // waiter的方法通过该方法发起调用
 * 	        waiter.greeTo(clientName);
 * 	        waiter.serveTo(clientName);
 * 	    }
 * 	    public void setWaiter(Waiter waiter) {
 * 	        this.waiter = waiter;
 * 	    }
 * 	}
 * }</pre>
 * 如果希望所有由WaiterDelegate#service()方法发起调用的其他方法都织入增强，就必须使用流程切面来完成目标。
 *
 * <p>流程切面和动态切面从某种程度上说可以算是一类切面，因为二者都需要在运行期判断动态的环境。对于流程切面来说，代理对象在每次调用目标类方法时，
 * 都需要判断方法调用堆栈中是否满足流程切点要求的方法。因此，和动态切面一样，流程切面对性能的影响也很大。
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

	private final Class<?> clazz;

	@Nullable
	private final String methodName;

	private final AtomicInteger evaluations = new AtomicInteger(0);


	/**
	 * Construct a new pointcut that matches all control flows below that class.
	 * @param clazz the clazz
	 */
	public ControlFlowPointcut(Class<?> clazz) {
		this(clazz, null);
	}

	/**
	 * Construct a new pointcut that matches all calls below the given method
	 * in the given class. If no method name is given, matches all control flows
	 * below the given class.
	 * @param clazz the clazz
	 * @param methodName the name of the method (may be {@code null})
	 */
	public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
		this.methodName = methodName;
	}


	/**
	 * Subclasses can override this for greater filtering (and performance).
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * Subclasses can override this if it's possible to filter out some candidate classes.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean isRuntime() {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		this.evaluations.incrementAndGet();

		for (StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.getClassName().equals(this.clazz.getName()) &&
					(this.methodName == null || element.getMethodName().equals(this.methodName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * It's useful to know how many times we've fired, for optimization.
	 */
	public int getEvaluations() {
		return this.evaluations.get();
	}


	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControlFlowPointcut)) {
			return false;
		}
		ControlFlowPointcut that = (ControlFlowPointcut) other;
		return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(this.methodName, that.methodName);
	}

	@Override
	public int hashCode() {
		int code = this.clazz.hashCode();
		if (this.methodName != null) {
			code = 37 * code + this.methodName.hashCode();
		}
		return code;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": class = " + this.clazz.getName() + "; methodName = " + methodName;
	}

}

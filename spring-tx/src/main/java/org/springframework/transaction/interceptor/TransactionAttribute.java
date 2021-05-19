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

package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 * @see DefaultTransactionAttribute
 * @see RuleBasedTransactionAttribute
 *
 * @apiNote 事务属性接口，该接口是TransactionDefinition的实现接口
 *
 * <p>获取TransactionAttribute
 * 在执行目标方法之前，获取事务之前，执行的目标增强。spring是从事务来源中获取事务的属性TransactionAttribute
 * spring 基于注解事务和声明事务的事务来源为： AnnotationTransactionAttributeSource、NameMatchTransactionAttributeSource
 * spring 基于注解事务和声明事务的TransactionDefinition默认实现都为是 RuleBasedTransactionAttribute
 */
public interface TransactionAttribute extends TransactionDefinition {

	/**
	 * Return a qualifier value associated with this transaction attribute.
	 * <p>This may be used for choosing a corresponding transaction manager
	 * to process this specific transaction.
	 * @since 3.0
	 *
	 * @apiNote 事务的目标方法的完整限定方法名称
	 */
	@Nullable
	String getQualifier();

	/**
	 * Should we roll back on the given exception?
	 * @param ex the exception to evaluate
	 * @return whether to perform a rollback or not
	 *
	 * @apiNote 对于给定的异常信息，是否回滚
	 */
	boolean rollbackOn(Throwable ex);

}

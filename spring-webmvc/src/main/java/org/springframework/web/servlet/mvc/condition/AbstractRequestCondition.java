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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * A base class for {@link RequestCondition} types providing implementations of
 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @param <T> the type of objects that this RequestCondition can be combined
 * with and compared to
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

	/**
	 * Indicates whether this condition is empty, i.e. whether or not it
	 * contains any discrete items.
	 * @return {@code true} if empty; {@code false} otherwise
	 *
	 * @apiNote 当前请求匹配条件对象中的内容是否为空。
	 */
	public boolean isEmpty() {
		return getContent().isEmpty();
	}

	/**
	 * Return the discrete items a request condition is composed of.
	 * <p>For example URL patterns, HTTP request methods, param expressions, etc.
	 * @return a collection of objects (never {@code null})
	 *
	 * @apiNote 一个请求匹配条件可能由多个部分组成，这些组成部分被包装成一个名为 content 的集合。
	 * 比如 ：
	 * 	对于请求路径匹配条件，可能有多个 URL pattern,
	 * 	对于请求方法匹配条件，可能有多个 HTTP request method,
	 * 	对于请求参数匹配条件，可能有多个 param 表达式.
	 */
	protected abstract Collection<?> getContent();

	/**
	 * The notation to use when printing discrete items of content.
	 * <p>For example {@code " || "} for URL patterns or {@code " && "}
	 * for param expressions.
	 *
	 * @apiNote 将该条件作为字符串展示时，各个组成部分之间的中缀标识符。比如 "||" 或者 "&&" 等。
	 */
	protected abstract String getToStringInfix();


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return getContent().equals(((AbstractRequestCondition<?>) other).getContent());
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		String infix = getToStringInfix();
		StringJoiner joiner = new StringJoiner(infix, "[", "]");
		for (Object expression : getContent()) {
			joiner.add(expression.toString());
		}
		return joiner.toString();
	}

}

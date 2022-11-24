/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * Contract for request mapping conditions.
 *
 * <p>Request conditions can be combined via {@link #combine(Object)}, matched to
 * a request via {@link #getMatchingCondition(HttpServletRequest)}, and compared
 * to each other via {@link #compareTo(Object, HttpServletRequest)} to determine
 * which is a closer match for a given request.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 * @param <T> the type of objects that this RequestCondition can be combined
 * with and compared to
 *
 * @apiNote 请求匹配条件。RequestCondition 接口是 SpringMVC 对一个请求匹配条件的概念建模。
 * RequestCondition 是一个泛型接口，事实上，它的泛型参数 T 通常也是一个 RequestCondition 对象。
 */
public interface RequestCondition<T> {

	/**
	 * Combine this condition with another such as conditions from a
	 * type-level and method-level {@code @RequestMapping} annotation.
	 * @param other the condition to combine with.
	 * @return a request condition instance that is the result of combining
	 * the two condition instances.
	 *
	 * @apiNote 将该请求匹配条件和另外一个请求匹配条件合并
	 */
	T combine(T other);

	/**
	 * Check if the condition matches the request returning a potentially new
	 * instance created for the current request. For example a condition with
	 * multiple URL patterns may return a new instance only with those patterns
	 * that match the request.
	 * <p>For CORS pre-flight requests, conditions should match to the would-be,
	 * actual request (e.g. URL pattern, query parameters, and the HTTP method
	 * from the "Access-Control-Request-Method" header). If a condition cannot
	 * be matched to a pre-flight request it should return an instance with
	 * empty content thus not causing a failure to match.
	 * @return a condition instance in case of a match or {@code null} otherwise.
	 *
	 * @apiNote 检查当前请求匹配条件和指定请求 request 是否匹配，如果不匹配返回 null，
	 * 如果匹配，生成一个新的请求匹配条件，该新的请求匹配条件是当前请求匹配条件针对指定请求 request 的剪裁。
	 *
	 * <p>举个例子来讲，如果当前请求匹配条件是一个路径匹配条件，包含多个路径匹配模板，并且其中有些模板和指定请求 request 匹配，
	 * 那么返回的新建的请求匹配条件将仅仅包含和指定请求 request 匹配的那些路径模板。
	 */
	@Nullable
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * Compare this condition to another condition in the context of
	 * a specific request. This method assumes both instances have
	 * been obtained via {@link #getMatchingCondition(HttpServletRequest)}
	 * to ensure they have content relevant to current request only.
	 */
	int compareTo(T other, HttpServletRequest request);

}

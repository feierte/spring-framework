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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to bind a method parameter to a session attribute.
 *
 * <p>The main motivation is to provide convenient access to existing, permanent
 * session attributes (e.g. user authentication object) with an optional/required
 * check and a cast to the target method parameter type.
 *
 * <p>For use cases that require adding or removing session attributes consider
 * injecting {@code org.springframework.web.context.request.WebRequest} or
 * {@code javax.servlet.http.HttpSession} into the controller method.
 *
 * <p>For temporary storage of model attributes in the session as part of the
 * workflow for a controller, consider using {@link SessionAttributes} instead.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see RequestMapping
 * @see SessionAttributes
 * @see RequestAttribute
 * <p>
 * 此注解是用于让开发者和Servlet API进行解耦
 * 让开发者可以无需使用HttpSession的getAttribute方法即可从会话域中获取数据。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SessionAttribute {

	/**
	 * Alias for {@link #name}.
	 * 用于指定在会话域中数据的名称。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the session attribute to bind to.
	 * <p>The default name is inferred from the method parameter name.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * Whether the session attribute is required.
	 * <p>Defaults to {@code true}, leading to an exception being thrown
	 * if the attribute is missing in the session or there is no session.
	 * Switch this to {@code false} if you prefer a {@code null} or Java 8
	 * {@code java.util.Optional} if the attribute doesn't exist.
	 * <p>
	 * 用于指定是否必须从会话域中获取到数据。默认值是true，表示如果指定名称不存在会报错。
	 */
	boolean required() default true;

}

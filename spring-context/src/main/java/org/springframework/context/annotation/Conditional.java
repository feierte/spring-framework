/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a component is only eligible for registration when all
 * {@linkplain #value specified conditions} match.
 *
 * <p>A <em>condition</em> is any state that can be determined programmatically
 * before the bean definition is due to be registered (see {@link Condition} for details).
 *
 * <p>The {@code @Conditional} annotation may be used in any of the following ways:
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype
 * annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Conditional},
 * all of the {@code @Bean} methods, {@link Import @Import} annotations, and
 * {@link ComponentScan @ComponentScan} annotations associated with that
 * class will be subject to the conditions.
 *
 * <p><strong>NOTE</strong>: Inheritance of {@code @Conditional} annotations
 * is not supported; any conditions from superclasses or from overridden
 * methods will not be considered. In order to enforce these semantics,
 * {@code @Conditional} itself is not declared as
 * {@link java.lang.annotation.Inherited @Inherited}; furthermore, any
 * custom <em>composed annotation</em> that is meta-annotated with
 * {@code @Conditional} must not be declared as {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see Condition
 *
 * @apiNote 条件注解。
 *
 * <p> @Conditional 执行时机？
 * 注解驱动的 Spring 编程模型中，注解主要包括两大块，其中一块是使用在配置类上，另一块是使用在配置类的 @Bean 标注的方法上。
 * 配置类是指存在注解 @Component、@Import、@ImportResource、@ComponentScan 或方法上存在 @Bean 的类，参见 {@link ConfigurationClassUtils#isConfigurationCandidate(AnnotationMetadata)}。
 * 应用中通常会指定一个 Spring 扫描 bean 的包名，Spring 将包中满足条件的类识别为配置类，然后对配置类进行解析，解析的结果可能包含新的配置类，Spring 反复处理，直到没有新的配置类。
 * 然后将所有的配置类注册为 Spring 中的 bean。
 * <b>@Conditional 的执行时机就包含在解析配置类和注册为 Bean 这两大阶段。<b/>
 * <br/>
 *
 * 如果我们希望在解析配置类时进行 @Conditional 判断，在注册 bean 时不再进行 @Conditional 判断，则需要使用 Condition 的子类 {@link ConfigurationCondition} 。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * All {@link Condition} classes that must {@linkplain Condition#matches match}
	 * in order for the component to be registered.
	 *
	 * @apiNote {@link Condition} 数组，该数组中的所有 Condition 对象满足条件时，该 bean 才会被注册。
	 */
	Class<? extends Condition>[] value();

}

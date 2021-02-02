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

/**
 * A {@link Condition} that offers more fine-grained control when used with
 * {@code @Configuration}. Allows certain conditions to adapt when they match
 * based on the configuration phase. For example, a condition that checks if a bean
 * has already been registered might choose to only be evaluated during the
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase}.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see Configuration
 *
 * <p>判断带有@Configuration注解的配置Class是否满足condition条件
 */
public interface ConfigurationCondition extends Condition {

	/**
	 * Return the {@link ConfigurationPhase} in which the condition should be evaluated.
	 */
	ConfigurationPhase getConfigurationPhase();


	/**
	 * The various configuration phases where the condition could be evaluated.
	 * <p>
	 * ConfigurationPhase控制的是过滤的时机，是在创建Configuration类的时候过滤还是在创建bean的时候过滤（也可用条件注解的生效阶段来描述）
	 *
	 * <p>默认情况下，带有@Configuration注解的类  为full configuration ， phase 为 PARSE_CONFIGURATION，
	 * 其余方法带有@Bean，或者注解元数据中 带有 @Component，@ComponentScan，@Import，@ImportResource的 为 lite 模式，phase 为REGISTER_BEAN
	 */
	enum ConfigurationPhase {

		/**
		 * The {@link Condition} should be evaluated as a {@code @Configuration}
		 * class is being parsed.
		 * <p>If the condition does not match at this point, the {@code @Configuration}
		 * class will not be added.
		 * <p>判断是否在解析配置类的时候就进行 condition 条件判断，若失败，则该配置类不注册
		 */
		PARSE_CONFIGURATION,

		/**
		 * The {@link Condition} should be evaluated when adding a regular
		 * (non {@code @Configuration}) bean. The condition will not prevent
		 * {@code @Configuration} classes from being added.
		 * <p>At the time that the condition is evaluated, all {@code @Configuration}
		 * classes will have been parsed.
		 * <p>所有配置类均注册为Bean
		 */
		REGISTER_BEAN
	}

}

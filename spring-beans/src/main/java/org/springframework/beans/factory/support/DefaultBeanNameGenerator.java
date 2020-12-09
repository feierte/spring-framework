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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Default implementation of the {@link BeanNameGenerator} interface, delegating to
 * {@link BeanDefinitionReaderUtils#generateBeanName(BeanDefinition, BeanDefinitionRegistry)}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
public class DefaultBeanNameGenerator implements BeanNameGenerator {

	/**
	 * A convenient constant for a default {@code DefaultBeanNameGenerator} instance,
	 * as used for {@link AbstractBeanDefinitionReader} setup.
	 * @since 5.2
	 * <p>这里将DefaultBeanNameGenerator设置为单例，是为了从节约内存开销考虑的。
	 * 但是设置为单例，却没有把构造器变为私有的？
	 * 这是有道理的，因为DefaultBeanNameGenerator是公有的API，
	 * 将DefaultBeanNameGenerator设置为单例模式是在5.2版本，而这个API在2.0.3版本就有了，
	 * 这样如果擅自在高本版里面把DefaultBeanNameGenerator构造函数设置为私有，其他版本会出现兼容方式的问题。
	 * 但是Spring官方推荐使用这个API还是通过单例的方式使用
	 */
	public static final DefaultBeanNameGenerator INSTANCE = new DefaultBeanNameGenerator();


	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return BeanDefinitionReaderUtils.generateBeanName(definition, registry);
	}

}

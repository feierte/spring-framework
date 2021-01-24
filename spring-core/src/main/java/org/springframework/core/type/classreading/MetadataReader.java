/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * Simple facade for accessing class metadata,
 * as read by an ASM {@link org.springframework.asm.ClassReader}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 *
 * <p>访问类元数据的入口
 * 元数据，是框架设计中必须的一个概念，所有的流行框架里都能看到它的影子，包括且不限于Spring、SpringBoot、SpringCloud、Mybatis、Hibernate等。
 * 它的作用肯定是大大的，它能模糊掉具体的类型，能让数据输出变得统一，能解决Java抽象解决不了的问题，比如运用的最广的便是注解，因为它不能继承，无法抽象，
 * 所以元数据方式就可以完美形成统一的向上抽取让它变得与类型无关，也就是常说的模糊效果，这便是矿建的核心设计思想。
 *
 * <p>
 * 为何Spring要为ClassMetadata和AnnotatedTypeMetadata提供一个标准实现（反射实现）和一个ASM的实现呢？这里就能给你答案。
 * 此接口是一个访问ClassMetadata等的简单门面，实现是委托给org.springframework.asm.ClassReader、ClassVisitor来处理的，
 * 它不用把Class加载进JVM就可以拿到元数据，因为它读取的是资源：Resource，这是它最大的优势所在。
 */
public interface MetadataReader {

	/**
	 * Return the resource reference for the class file.
	 * <p>返回此Class文件的来源（资源）
	 */
	Resource getResource();

	/**
	 * Read basic class metadata for the underlying class.
	 * <p>
	 * 返回此Class的元数据信息
	 */
	ClassMetadata getClassMetadata();

	/**
	 * Read full annotation metadata for the underlying class,
	 * including metadata for annotated methods.
	 * <p>
	 * 返回此类的注解元信息（包括方法的注解元信息）
	 */
	AnnotationMetadata getAnnotationMetadata();

}

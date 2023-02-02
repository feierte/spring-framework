package org.springframework.demo.metadata.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jie Zhao
 * @date 2023/1/31 21:40
 */
@TransactionalService(name = "test")
public class AnnotationMetadataDemo {

	public static void main(String[] args) throws IOException {

		// test1();
		// test2();
//		test3();
		test4();
	}


	public static void test1() throws IOException {
		String className = AnnotationMetadataDemo.class.getName();
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
		AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
		annotationMetadata.getAnnotationTypes().forEach(annotationType -> {
			Set<String> metaAnnotationTypes = annotationMetadata.getMetaAnnotationTypes(annotationType);
			metaAnnotationTypes.forEach(metaAnnotationType -> {
				System.out.printf("注解 @%s 的元注解为 @%s\n", annotationType, metaAnnotationType);
			});
		});
	}

	public static void test2() {
		AnnotatedElement annotatedElement = AnnotationMetadataDemo.class;
		TransactionalService transactionalService = annotatedElement.getAnnotation(TransactionalService.class);
		String value = transactionalService.name();
		System.out.println("TransactionalService.name() = " + value);
	}


	/**
	 * 使用反射实现
	 */
	public static void test3() {
		AnnotatedElement annotatedElement = AnnotationMetadataDemo.class;
		TransactionalService transactionalService = annotatedElement.getAnnotation(TransactionalService.class);
		// 完全 Java 反射实现
		ReflectionUtils.doWithMethods(TransactionalService.class,
				method -> System.out.printf("@TransactionalService.%s() = %s\n",
						method.getName(),
						// 执行 Method 反射调用
						ReflectionUtils.invokeMethod(method, transactionalService)),
				// 选择无参数方法
				method -> method.getParameterCount() == 0);
	}


	public static void test4() {
		AnnotatedElement annotatedElement = AnnotationMetadataDemo.class;
		TransactionalService transactionalService = annotatedElement.getAnnotation(TransactionalService.class);
		// 获取 transactionalService 上的所有元注解
		Set<Annotation> metaAnnotations = getAllMetaAnnotations(transactionalService);
		// 输出结果
		metaAnnotations.forEach(AnnotationMetadataDemo::printAnnotationAttribute);
	}

	private static void printAnnotationAttribute(Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		// 完全 Java 反射实现
		ReflectionUtils.doWithMethods(annotationType,
				method -> System.out.printf("@%s.%s() = %s\n",
						annotationType.getSimpleName(),
						method.getName(),
						// 执行 Method 反射调用
						ReflectionUtils.invokeMethod(method, annotation)),
				// 选择无参数方法
				method -> method.getParameterCount() == 0);
	}

	private static Set<Annotation> getAllMetaAnnotations(Annotation annotation) {

		Annotation[] metaAnnotations = annotation.annotationType().getAnnotations();
		if (ObjectUtils.isEmpty(metaAnnotations)) {
			return Collections.emptySet();
		}

		Set<Annotation> metaAnnotationSet = Stream.of(metaAnnotations)
				// 排除 Java 标准元注解，例如 @Target，@Documented等，他们因相互依赖，将导致递归不断
				// 通过 java.lang.annotation 包名排除
				.filter(metaAnnotation -> !Target.class.getPackage().equals(metaAnnotation.annotationType().getPackage()))
				.collect(Collectors.toSet());

		// 递归查找元注解的元注解集合
		HashSet<Annotation> collect = metaAnnotationSet.stream()
				.map(AnnotationMetadataDemo::getAllMetaAnnotations)
				.collect(HashSet::new, Set::addAll, Set::addAll);

		// 添加递归结果
		metaAnnotationSet.addAll(collect);
		return metaAnnotationSet;
	}
}

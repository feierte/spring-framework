package org.springframework.demo.config;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

/**
 * @author Jie Zhao
 * @date 2020/10/27 21:18
 */
public class CustomBeanNameGenerator implements BeanNameGenerator {

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		// 判断当前bean的定义信息是否是注解
		if (definition instanceof AnnotatedBeanDefinition) {
			// 强转成AnnotatedBeanDefinition
			AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) definition;

			// 获取注解Bean定义的元信息
			AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();

			// 获取元信息中的所有注解
			Set<String> annotationTypes = metadata.getAnnotationTypes();
		}
		return null;
	}
}

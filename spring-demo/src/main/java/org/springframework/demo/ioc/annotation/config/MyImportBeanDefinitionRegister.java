package org.springframework.demo.ioc.annotation.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

/**
 * @author Jie Zhao
 * @date 2020/11/2 20:00
 */
public class MyImportBeanDefinitionRegister implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		List<String> basePackages = null;

		if (importingClassMetadata.hasAnnotation(ComponentScan.class.getName())) {

		}
	}
}

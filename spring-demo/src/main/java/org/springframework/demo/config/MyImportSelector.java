package org.springframework.demo.config;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author Jie Zhao
 * @date 2020/10/28 22:06
 */
public class MyImportSelector implements ImportSelector {

	/**
	 * 导入规则的  Aspectj表达式
	 */
	private String aspectjExpression;

	public MyImportSelector() {
		try {
			Properties props = PropertiesLoaderUtils.loadAllProperties("importSelector.properties");

			aspectjExpression = props.getProperty("importSelector.expression");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param importingClassMetadata
	 * @return
	 */
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {

		String[] basePackages = null;

		/*
		 * 如果有@ComponentScan注解，就扫描@ComponentScan注解中指定的包
		 */
		if (importingClassMetadata.hasAnnotation(ComponentScan.class.getName())) {
			// 取出ComponentScan中的所有属性及属性值
			MultiValueMap<String, Object> allAnnotationAttributes =
					importingClassMetadata.getAllAnnotationAttributes(ComponentScan.class.getName());
			// 取出basePackages属性中的值
			basePackages = (String[]) allAnnotationAttributes.get("basePackages")
					.toArray(new String[allAnnotationAttributes.size()]);
		}

		/*
		 * 如果没有@ComponentScan注解，就扫描@Import注解的类 所在的包
		 */
		if (basePackages == null || basePackages.length == 0) {
			String basePackage = null;
			try {
				// 获取@Import注解的类所在包的包名
				basePackage = Class.forName(importingClassMetadata.getClassName()).getPackage().getName();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			// 把包名放进basePackages中
			basePackages = new String[]{basePackage};
		}

		// 创建类路径扫描器
		ClassPathScanningCandidateComponentProvider scanner =
				new ClassPathScanningCandidateComponentProvider(false);
		// 创建类型过滤器
		TypeFilter filter = new AspectJTypeFilter(aspectjExpression, MyImportSelector.class.getClassLoader());
		scanner.addIncludeFilter(filter);

		Set<String> classes = new HashSet<>();
		Arrays.stream(basePackages).forEach(basePackage -> {
			scanner.findCandidateComponents(basePackage)
					.forEach(beanDefinition -> classes.add(beanDefinition.getBeanClassName()));
		});

		return classes.toArray(new String[classes.size()]);
	}
}

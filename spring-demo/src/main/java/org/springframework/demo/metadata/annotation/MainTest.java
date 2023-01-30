package org.springframework.demo.metadata.annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

/**
 * @author Jie Zhao
 * @date 2023/1/30 21:55
 */
public class MainTest {

	public static void main(String[] args) {
		AnnotationMetadata annotationMetadata = AnnotationMetadata.introspect(ControllerDemo.class);
		boolean annotated = annotationMetadata.isAnnotated(Component.class.getName());
		System.out.println(annotated);

		boolean hasAnnotation = annotationMetadata.hasAnnotation(Component.class.getName());
		System.out.println(hasAnnotation);
	}
}

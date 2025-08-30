package org.springframework.demo.util.metadata.annotation;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Controller;

/**
 * @author Jie Zhao
 * @date 2021/11/9 11:39
 *
 * @see MergedAnnotations
 * @see org.springframework.core.annotation.MergedAnnotation
 */
public class MergedAnnotationsDemo {

	public static void main(String[] args) {
		MergedAnnotations mergedAnnotations = MergedAnnotations.from(ControllerDemo.class);

		boolean present = mergedAnnotations.isPresent(Controller.class);
		System.out.println(present);
	}



}

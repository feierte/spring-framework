package org.springframework.demo.util.metadata.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author Jie Zhao
 * @date 2023/1/30 22:02
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StringRepository {

	/**
	 * 属性方法名称必须与 {@link Component#value()} 保持一致
	 * @return Bean 的名称
	 */
	String value() default "";
}

package org.springframework.demo.mvc.shangguigu;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * @author jie zhao
 * @date 2022/11/29 22:31
 */
@Documented
@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseJson {

	String value() default "";
}

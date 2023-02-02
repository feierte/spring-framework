package org.springframework.demo.metadata.annotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * @author Jie Zhao
 * @date 2023/1/31 21:40
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
@Service
public @interface TransactionalService {

	/**
	 * 指定 Bean 的名称
	 * @return
	 */
	String name() default "";

	/**
	 * 覆盖 {@link Transactional#transactionManager()} 默认值
	 * @return {@link org.springframework.transaction.PlatformTransactionManager} Bean 名称，默认关联 “txManager” Bean
	 */
	// String transactionManager() default "txManager";


	/**
	 * 覆盖 {@link Transactional#value()} 默认值
	 * @return {@link org.springframework.transaction.PlatformTransactionManager} Bean 名称，默认关联 “txManager” Bean
	 */
	String value() default "txManager";
}

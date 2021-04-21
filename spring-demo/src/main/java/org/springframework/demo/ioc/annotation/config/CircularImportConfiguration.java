package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 测试循环import问题
 * @author jie zhao
 * @date 2021/2/4 10:41
 */
//@Configuration
public class CircularImportConfiguration {

	@Import(B.class)
	class A {}

	@Import(A.class)
	class B {}
}

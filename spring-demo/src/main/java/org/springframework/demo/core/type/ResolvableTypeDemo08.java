package org.springframework.demo.core.type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 真实 Spring 环境验证（推荐用 Spring Boot 测试）
 *
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */
//@SpringBootTest
public class ResolvableTypeDemo08 {
	@Autowired
	private List<String> stringList;          // 会注入所有 String 类型的 bean？注意实际行为

	// 更好的例子：
	@Autowired
	private Store<String> stringStore;

	@Autowired
	private Store<Integer> integerStore;

	//		@Test
	void testGenericInjection() {
		// 验证两个 Store 是否被正确区分注入
	}
}

interface Store<T> {
}

@Component
class StringStore implements Store<String> {
}

@Component
class IntegerStore implements Store<Integer> {
}

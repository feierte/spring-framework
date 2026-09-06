package org.springframework.demo.core.type;

import java.util.List;
import java.util.Map;

/**
 * 解析类型变量（继承场景）
 *
 * @author Jie Zhao
 * @date 2026/9/6 10:02
 */
public class ResolvableTypeDemo02 {

	public static void main(String[] args) throws Exception {
		// TODO: 用反射拿到 ServiceImpl 实现的接口的真实泛型参数（String 和 Integer）。

	}

	interface Service<T, R> {}
	class ServiceImpl implements Service<String, Integer> {}  // 注意这里是直接继承接口

	// 或更复杂：
	class Base<T> {}
	class Sub extends Base<List<String>> {}
}

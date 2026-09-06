package org.springframework.demo.core.type;

import java.util.List;
import java.util.Map;

/**
 * 手动解析简单泛型字段
 *
 * @author Jie Zhao
 * @date 2026/9/6 10:02
 */
public class ResolvableTypeDemo01 {

	private List<String> list;
	private Map<Integer, List<String>> nestedMap;
	private String[] array;

	public static void main(String[] args) throws Exception {
		// TODO: 分别解析上面三个字段的 getGenericType()
		// 要求打印：
		// 1. 是否是 ParameterizedType
		// 2. rawType
		// 3. 所有 actualTypeArguments
		// 4. 如果是数组，打印组件类型
	}
}

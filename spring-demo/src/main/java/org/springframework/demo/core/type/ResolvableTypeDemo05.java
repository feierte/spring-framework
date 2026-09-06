package org.springframework.demo.core.type;

import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */
public class ResolvableTypeDemo05 {

	private List<Map<String, List<Integer>>> complex;


	public static void main(String[] args) throws Exception {
		// 任务：复杂嵌套 + isAssignableFrom
		// 1. 用 resolveGeneric 一路钻进去拿到 Integer
		// 2. 判断这个类型是否可赋值给 List<?>
		// 3. 判断是否可赋值给 List<Map<String, List<Number>>>（注意泛型不变性）
	}
}

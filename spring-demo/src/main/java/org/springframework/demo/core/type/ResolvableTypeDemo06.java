package org.springframework.demo.core.type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

import java.util.List;
import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */
public class ResolvableTypeDemo06 {

	ResolvableType type = ResolvableType.forType(
			new ParameterizedTypeReference<List<Map<String, Integer>>>() {});


	public static void main(String[] args) throws Exception {
		// 任务：ParameterizedTypeReference（保留运行时泛型）

	}
}

package org.springframework.demo.core.type;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 模拟依赖注入中的泛型匹配
 *
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */
public class ResolvableTypeDemo07 {

	private List<String> stringList;
	private List<Integer> integerList;

	public static void main(String[] args) throws Exception {
		Field stringField = ResolvableTypeDemo07.class.getDeclaredField("stringList");
		Field integerField = ResolvableTypeDemo07.class.getDeclaredField("integerList");

		DependencyDescriptor stringDesc = new DependencyDescriptor(stringField, false);
		DependencyDescriptor integerDesc = new DependencyDescriptor(integerField, false);

		ResolvableType stringType = stringDesc.getResolvableType();
		ResolvableType integerType = integerDesc.getResolvableType();

		// 任务：
		// 1. 打印两个 ResolvableType
		// 2. 判断 stringType.isAssignableFrom(integerType) 的结果
		// 3. 自己实现一个简单的“是否匹配”逻辑（只判断泛型参数是否完全一致）
	}
}

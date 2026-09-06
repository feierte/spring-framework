package org.springframework.demo.core.type;

import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;

/**
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */
public class ResolvableTypeDemo03 {

	private HashMap<Integer, List<String>> myMap;
	private List<String>[] arrayOfList;
	private List<? extends Number> wildcardList;

	public static void main(String[] args) throws Exception {
		ResolvableType t = ResolvableTypeUtils.forField(ResolvableTypeEx1.class, "myMap");

		// 任务：
		// 1. 打印 t.getSuperType()
		// 2. 打印 t.asMap()
		// 3. 打印 t.getGeneric(0).resolve()          → Integer
		// 4. 打印 t.getGeneric(1).resolve()          → List
		// 5. 打印 t.resolveGeneric(1, 0)             → String
		// 6. 对 arrayOfList 和 wildcardList 做同样探索
	}
}

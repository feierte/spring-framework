package org.springframework.demo.core.type;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;

/**
 * @author Jie Zhao
 * @date 2026/9/6 10:05
 */
public class ResolvableTypeUtils {

	public static void printType(ResolvableType type, String prefix) {
		System.out.println(prefix + "Type: " + type);
		System.out.println(prefix + "resolve(): " + type.resolve());
		System.out.println(prefix + "hasGenerics: " + type.hasGenerics());
		if (type.hasGenerics()) {
			ResolvableType[] generics = type.getGenerics();
			for (int i = 0; i < generics.length; i++) {
				System.out.println(prefix + "  generic[" + i + "]: " + generics[i]);
				printType(generics[i], prefix + "    ");
			}
		}
	}

	public static ResolvableType forField(Class<?> clazz, String fieldName) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);
		return ResolvableType.forField(field);
	}
}

package org.springframework.demo.javabeans;

import org.springframework.core.MethodParameter;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.demo.ioc.beanfactory.City;
import org.springframework.demo.ioc.beanfactory.User;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jie Zhao
 * @date 2021/10/27 18:00
 *
 * @see org.springframework.core.MethodParameter
 */
public class MethodParameterDemo {

	public static void main(String[] args) throws NoSuchMethodException {
		User user = new User(10, "张三", 18);
		user.setWorkCities(new City[]{City.BEIJING, City.HANGZHOU});
		user.setLifeCities(Arrays.asList(City.BEIJING, City.HANGZHOU));

		// Method method = user.getClass().getMethod("setWorkCities", City[].class);
		Method method = user.getClass().getMethod("setLifeCities", List.class);

		MethodParameter methodParameter = new MethodParameter(method, 0);
		Parameter parameter = methodParameter.getParameter();
		System.out.println(parameter);

		Class<?> nestedParameterType = methodParameter.getNestedParameterType();
		System.out.println(nestedParameterType);

		methodParameter.initParameterNameDiscovery(new StandardReflectionParameterNameDiscoverer());
		String parameterName = methodParameter.getParameterName();
		System.out.println("参数名：" + parameterName);
		// log.info("sss");
	}
}

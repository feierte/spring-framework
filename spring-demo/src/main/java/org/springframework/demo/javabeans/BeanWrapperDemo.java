package org.springframework.demo.javabeans;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.demo.ioc.beanfactory.City;
import org.springframework.demo.ioc.beanfactory.User;

import java.util.*;

/**
 * @author Jie Zhao
 * @date 2021/10/28 9:59
 *
 * @see org.springframework.beans.BeanWrapper
 * @see org.springframework.beans.BeanWrapperImpl
 */
public class BeanWrapperDemo {

	public static void main(String[] args) throws NoSuchMethodException {
		User user = new User(10, "张三", 18);
		user.setWorkCities(new City[]{City.BEIJING, City.HANGZHOU});
		user.setLifeCities(Arrays.asList(City.BEIJING, City.HANGZHOU));

		List<Map<String, Object>> cities = new ArrayList<>();
		Map<String, Object> map = new HashMap<>();
		map.put("aaa", "北京");
		map.put("bbb", "上海");
		cities.add(map);
		user.setCities(cities);

		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);

		Object propertyValue = beanWrapper.getPropertyValue("lifeCities[0]");
		System.out.println(propertyValue);

		Object propertyValue1 = beanWrapper.getPropertyValue("cities[0][bbb]");
		System.out.println(propertyValue1);
	}
}

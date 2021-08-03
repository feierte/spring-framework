package org.springframework.demo.ioc.beanfactory.bean;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.demo.ioc.beanfactory.City;
import org.springframework.demo.ioc.beanfactory.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PropertyAccessorDemo {

	public static void main(String[] args) {

		User user = new User();
		Map<String, Object> map = new HashMap<>();
		map.put("name", "张三");
		map.put("address", "nanjing");

		PropertyAccessor propertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(user);

		System.out.println(propertyAccessor.isReadableProperty("lifeCities"));
		System.out.println(propertyAccessor.isWritableProperty("lifeCities"));
		System.out.println(propertyAccessor.getPropertyValue("lifeCities"));

		ArrayList<City> cities = new ArrayList<>();
		cities.add(City.BEIJING);
		cities.add(City.HANGZHOU);
		propertyAccessor.setPropertyValue("lifeCities", cities);
		System.out.println(propertyAccessor.getPropertyValue("lifeCities"));
		System.out.println(propertyAccessor.getPropertyValue("lifeCities[1]"));
	}
}

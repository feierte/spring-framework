package org.springframework.demo.ioc.beanfactory.introspector;

import org.springframework.demo.domain.Person;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Jie Zhao
 * @date 2020/12/13 12:24
 */
public class PropertyDescriptorDemo {

	public static void main(String[] args) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		/*BeanInfo beanInfo = Introspector.getBeanInfo(Person.class);
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		System.out.println(propertyDescriptors.length);
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Method readMethod = propertyDescriptor.getReadMethod();
			Method writeMethod = propertyDescriptor.getWriteMethod();
			System.out.println(propertyDescriptor.getPropertyType());
			System.out.println("getter方法：" + readMethod.getName());
			if (writeMethod != null) {
				System.out.println("setter方法" + writeMethod.getName());
			}
		}*/

		test();
	}



	public static void test() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		Person person = new Person();
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor("name", Person.class);
		Method writeMethod = propertyDescriptor.getWriteMethod();
		writeMethod.invoke(person, "张三");
		System.out.println(person);
	}
}

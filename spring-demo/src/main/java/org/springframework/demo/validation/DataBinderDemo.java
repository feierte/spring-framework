package org.springframework.demo.validation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.demo.domain.Person;
import org.springframework.validation.DataBinder;

public class DataBinderDemo {

	public static void main(String[] args) {
		Person person = new Person();
		System.out.println(person);
		DataBinder dataBinder = new DataBinder(person, "person");

		// 创建用于绑定到对象上的属性对（属性名称，属性值）
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "fsx");
		pvs.add("age", 18);

		dataBinder.bind(pvs);

		System.out.println(person);

	}
}

package org.springframework.demo.ioc.beanfactory;

/**
 * @author Jie Zhao
 * @date 2020/12/17 22:42
 */
public class Company {

	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Company{" +
				"name='" + name + '\'' +
				'}';
	}
}

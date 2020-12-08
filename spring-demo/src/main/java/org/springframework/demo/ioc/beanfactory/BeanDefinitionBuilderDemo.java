package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * @author Jie Zhao
 * @date 2020/12/6 11:53
 */
public class BeanDefinitionBuilderDemo {

	public static void main(String[] args) {


	}

	// 通过BeanDefinitionBuilder构建BeanDefinition
	public static void getBeanDefinitionWithBuilder() {
		// 通过BeanDefinitionBuilder构建BeanDefinition
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class);

		// 通过属性设置
		beanDefinitionBuilder.addPropertyValue("id", 1);
		beanDefinitionBuilder.addPropertyValue("name", "小马哥");

		// 获取BeanDefinition实例
		// BeanDefinition并非Bean的终态， 可以自定义修改
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
	}

	// 通过AbstractBeanDefinition 及其派生类构造 BeanDefinition
	public static void getBeanDefinition() {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(User.class);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("id", 1);
		propertyValues.addPropertyValue("name", "小马哥");
		beanDefinition.setPropertyValues(propertyValues);
	}
}

package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * @author Jie Zhao
 * @date 2020/12/6 11:53
 */
public class BeanDefinitionBuilderDemo {

	public static void main(String[] args) {

		// 通过BeanDefinitionBuilder构建BeanDefinition
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class);
	}
}

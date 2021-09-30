package org.springframework.demo.ioc.beanfactory.bean.instantiation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.ioc.beanfactory.User;

/**
 * 使用 Supplier 方式进行 Bean 的实例化
 *
 * @author Jie Zhao
 * @date 2021/9/30 16:08
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])
 */
public class SupplierBeanDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(User.class);
		beanDefinition.setInstanceSupplier(User::createUser);
		String beanName = User.class.getSimpleName();
		applicationContext.registerBeanDefinition(beanName, beanDefinition);

		applicationContext.refresh();

		User user = (User) applicationContext.getBean(beanName);
		System.out.println(user);
	}
}

package org.springframework.demo.ioc.configuration.metadata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.demo.ioc.beanfactory.User;
import org.springframework.util.ObjectUtils;

/**
 * @author Jie Zhao
 * @date 2021/1/4 20:07
 */
public class BeanConfigurationMetadataDemo {

	public static void main(String[] args) {
		// BeanDefinition 的定义（声明）
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class);
		beanDefinitionBuilder.addPropertyValue("name", "mercyblitz");
		// 获取 AbstractBeanDefinition
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		// 附加属性（不影响 Bean populate、initialize）
		beanDefinition.setAttribute("name", "小马哥");
		// 当前 BeanDefinition 来自于何方（辅助作用）
		beanDefinition.setSource(BeanConfigurationMetadataDemo.class);

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (ObjectUtils.nullSafeEquals("user", beanName) && User.class.equals(bean.getClass())) {
					BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
					if (BeanConfigurationMetadataDemo.class.equals(bd.getSource())) { // 通过 source 判断来
						// 属性（存储）上下文
						String name = (String) bd.getAttribute("name"); // 就是 "小马哥"
						User user = (User) bean;
						user.setName(name);
					}
				}
				return bean;
			}
		});

		// 注册 User 的 BeanDefinition
		beanFactory.registerBeanDefinition("user", beanDefinition);

		User user = beanFactory.getBean("user", User.class);

		System.out.println(user);
	}
}

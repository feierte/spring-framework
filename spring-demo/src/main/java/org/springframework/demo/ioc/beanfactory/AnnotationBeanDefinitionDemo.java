package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Jie Zhao
 * @date 2020/12/9 20:43
 */
@Import(AnnotationBeanDefinitionDemo.Config.class)
public class AnnotationBeanDefinitionDemo {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		// 注册配置类（@Configuration）
		applicationContext.register(Config.class);
		applicationContext.refresh();

		// 按照类型查找
		System.out.println("Config类型的所有beans：" + applicationContext.getBeansOfType(Config.class));
		System.out.println("User类型的所有beans：" + applicationContext.getBeansOfType(User.class));

		applicationContext.close();
	}

	/**
	 * 命名方式（有名称的Bean）的注册方式
	 * @see BeanDefinitionRegistry#registerBeanDefinition(String, BeanDefinition)
	 * @param registry
	 * @param beanName
	 */
	public static void registerBeanDefinition(BeanDefinitionRegistry registry, String beanName) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class);
		beanDefinitionBuilder.addPropertyValue("id", 1)
				.addPropertyValue("name", "xiaomage");
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		if (StringUtils.hasText(beanName)) {
			registry.registerBeanDefinition(beanName, beanDefinition);
		} else {
			BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
		}
	}


	/**
	 * 非命名方式（Spring自动生成名称）
	 * @see BeanDefinitionReaderUtils#registerWithGeneratedName(AbstractBeanDefinition, BeanDefinitionRegistry)
	 * @param registry
	 */
	public static void registerBeanDefinition(BeanDefinitionRegistry registry) {
		registerBeanDefinition(registry, null);
	}

	@Component
	public static class Config {

		/**
		 * 通过注解方式，定义了一个Bean
		 * @return
		 */
		@Bean({"user", "xiaomage"})
		public User user(){
			User user = new User();
			user.setId(1);
			user.setName("xiaomage");
			return user;
		}
	}
}

package org.springframework.demo.util.metadata.annotation.override;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2023/2/2 20:15
 */
@Configuration
// 扫描 TransactionalServiceBean 所在的 package
@ComponentScan(basePackageClasses = TransactionalServiceBean.class)
@EnableTransactionManagement // 激活事务
public class AnnotationAttributeOverrideDemo {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(AnnotationAttributeOverrideDemo.class);

		Map<String, TransactionalServiceBean> beansMap = applicationContext.getBeansOfType(TransactionalServiceBean.class);
		beansMap.forEach((beanName, bean) -> {
			System.out.printf("Bean 名称：%s, 对象：%s\n", beanName, bean);
			bean.save();
		});
		applicationContext.close();
	}

	@Bean("txManager")
	public PlatformTransactionManager txManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				System.out.println("txManager: 事务提交...");
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				System.out.println("txManager: 事务回滚...");
			}
		};
	}

	@Bean("txManager2")
	public PlatformTransactionManager txManager2() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				System.out.println("txManager2: 事务提交...");
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				System.out.println("txManager2: 事务回滚...");
			}
		};
	}
}

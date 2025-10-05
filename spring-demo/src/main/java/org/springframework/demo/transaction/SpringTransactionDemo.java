package org.springframework.demo.transaction;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringTransactionDemo {

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(TransactionConfig.class);

		TransactionService transactionService = context.getBean("transactionService", TransactionService.class);
		transactionService.testTransaction();
	}
}

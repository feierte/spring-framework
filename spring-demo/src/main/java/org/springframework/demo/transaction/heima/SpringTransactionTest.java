package org.springframework.demo.transaction.heima;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.demo.transaction.heima.config.SpringConfiguration;
import org.springframework.demo.transaction.heima.service.AccountService;
import org.springframework.stereotype.Component;

/**
 * @author 黑马程序员
 * @Company http://www.itheima.com
 */
@Component
public class SpringTransactionTest {

	@Autowired
	private AccountService accountService;

	public void testTransfer() {
		accountService.transfer("aaa", "bbb", 100d);
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
		SpringTransactionTest springTransactionTest = applicationContext.getBean("springTransactionTest", SpringTransactionTest.class);
		springTransactionTest.testTransfer();
	}
}

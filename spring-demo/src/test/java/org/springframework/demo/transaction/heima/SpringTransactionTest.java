package org.springframework.demo.transaction.heima;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.demo.transaction.heima.config.SpringConfiguration;
import org.springframework.demo.transaction.heima.service.AccountService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author 黑马程序员
 * @Company http://www.itheima.com
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfiguration.class)
public class SpringTransactionTest {

	@Autowired
	private AccountService accountService;

	@Test
	public void testTransfer() {
		accountService.transfer("aaa", "bbb", 100d);
	}
}

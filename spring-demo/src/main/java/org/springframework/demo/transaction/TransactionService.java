package org.springframework.demo.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

	@Transactional
	public void testTransaction() {

	}
}

package org.springframework.demo.transaction;

import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import java.lang.reflect.Method;

@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public class TransactionAttributeSourceTest {

	public void m1() {

	}

	public static void main(String[] args) throws Exception {
		Method m1 = TransactionAttributeSourceTest.class.getMethod("m1");

		TransactionAttributeSource tas = new AnnotationTransactionAttributeSource();
		TransactionAttribute ta = tas.getTransactionAttribute(m1, TransactionAttributeSourceTest.class);
		System.out.println(ta);
	}
}

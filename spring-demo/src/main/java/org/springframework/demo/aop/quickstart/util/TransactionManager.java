package org.springframework.demo.aop.quickstart.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * @author Jie Zhao
 * @date 2020/11/16 20:48
 */
@Component
public class TransactionManager {

	@Autowired
	private Connection connection;

	public void begin() {
		try {
			connection.setAutoCommit(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commit() {
		try {
			connection.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void rollback() {
		try {
			connection.rollback();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

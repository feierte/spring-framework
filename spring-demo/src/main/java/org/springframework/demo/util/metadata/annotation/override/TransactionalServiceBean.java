package org.springframework.demo.util.metadata.annotation.override;

import org.springframework.demo.util.metadata.annotation.TransactionalService;

/**
 * @author Jie Zhao
 * @date 2023/2/2 20:14
 */
@TransactionalService
public class TransactionalServiceBean {

	public void save() {
		System.out.println("保存操作...");
	}
}

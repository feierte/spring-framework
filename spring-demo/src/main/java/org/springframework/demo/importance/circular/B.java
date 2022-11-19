package org.springframework.demo.importance.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jie zhao
 * @date 2022/11/19 15:46
 */
@Component
public class B {

	private A a;

	@Autowired
	public void setA(A a) {
		this.a = a;
	}
}

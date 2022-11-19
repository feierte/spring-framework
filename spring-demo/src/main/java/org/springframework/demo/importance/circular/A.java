package org.springframework.demo.importance.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jie zhao
 * @date 2022/11/19 15:46
 */
@Component
public class A {

	private B b;

	@Autowired
	public void setB(B b) {
		this.b = b;
	}
}

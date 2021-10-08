package org.springframework.demo.transaction.heima.domain;

import java.io.Serializable;

/**
 * @author Jie Zhao
 * @date 2021/10/7 22:39
 */
public class Account implements Serializable {

	private static final long serialVersionUID = -4431005869172636777L;
	private Integer id;
	private String name;
	private Double money;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}
}

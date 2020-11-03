package org.springframework.demo.ioc.annotation.service.impl;

import org.springframework.demo.ioc.annotation.service.UserService;

//@Service("userService")
public class UserServiceImpl implements UserService {


	@Override
	public void saveUser() {
		System.out.println("保存用户");
	}
}

package org.springframework.demo.service.impl;

import org.springframework.demo.service.UserService;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl implements UserService {


	@Override
	public void saveUser() {
		System.out.println("保存用户");
	}
}

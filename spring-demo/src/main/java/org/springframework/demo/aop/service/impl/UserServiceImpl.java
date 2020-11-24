package org.springframework.demo.aop.service.impl;


import org.springframework.demo.aop.domain.User;
import org.springframework.demo.aop.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author Jie Zhao
 * @date 2020/11/24 20:34
 */
@Service("userService1")
public class UserServiceImpl implements UserService {

	@Override
	public void saveUser(User user) {
		System.out.println("执行了保存用户：" + user);
	}
}

package org.springframework.demo.aop.quickstart.service.impl;


import org.springframework.aop.framework.AopContext;
import org.springframework.demo.aop.quickstart.domain.User;
import org.springframework.demo.aop.quickstart.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

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

	@Override
	public void saveAllUser(List<User> users) {
		for (User user : users) {
			UserService proxyUserService = (UserService) AopContext.currentProxy();
			proxyUserService.saveUser(user);
		}
	}

}

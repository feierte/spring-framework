package org.springframework.demo.aop.quickstart.service;

import org.springframework.demo.aop.quickstart.domain.User;

import java.util.List;

/**
 * @author Jie Zhao
 * @date 2020/11/24 20:34
 */
public interface UserService {

	void saveUser(User user);

	void saveAllUser(List<User> users);
}

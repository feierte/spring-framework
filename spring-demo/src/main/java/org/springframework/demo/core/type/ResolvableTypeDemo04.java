package org.springframework.demo.core.type;

import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;

/**
 * @author Jie Zhao
 * @date 2026/9/6 10:06
 */

class User {
}

interface Repository<T, ID> {
}

class UserRepository implements Repository<User, Long> {
}

class BaseService<T> {
	private List<T> data;
}

class UserService extends BaseService<User> {
}

public class ResolvableTypeDemo04 {
	public static void main(String[] args) throws Exception {
		// 1. 解析 UserRepository 实现的 Repository 接口的真实泛型
		ResolvableType repoType = ResolvableType.forClass(UserRepository.class)
				.as(Repository.class);
		// 期望：getGeneric(0) → User，getGeneric(1) → Long

		// 2. 解析 UserService 中 data 字段的真实类型（应该是 List<User>）
		// 提示：先拿到 UserService 的 ResolvableType，再找字段
	}
}

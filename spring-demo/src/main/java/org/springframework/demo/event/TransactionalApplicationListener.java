package org.springframework.demo.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * spring 支持事务和事件一起使用
 *
 * @author Jie Zhao
 * @date 2024/9/21 9:18
 */
@Component
public class TransactionalApplicationListener {

	/*
	 * 场景：用户注册成功后发布登录事件，但在后续的事务处理中处理异常导致事务回滚，会出现用户收到注册成功短信但实际没有注册成功。
	 * 解决方案：
	 * 	方案一：将事务处理逻辑和事件发布拆分，避免上述异常场景（推荐）
	 *	方案二：使用 TransactionalEventListener 指定和事务执行的顺序关系
	 *
	 */

	// 事务提交后才会执行相应事件
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserRegisterEvent(RegisterEvent event) throws Exception {
		String username = event.getUserName();
		// 在这里执行处理用户注册事件的逻辑，例如记录日志或触发其他操作
		System.out.println("User register: " + username);
	}
}

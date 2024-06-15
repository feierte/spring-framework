package org.springframework.demo.concurrent.scheduling.async;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.sql.Time;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jie Zhao
 * @date 2024/6/15 9:51
 */
//下面两个注解可以用@SpringJunitConfig(AsyncConfiguration.class)替代
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AsyncConfiguration.class)
@Slf4j
public class AsyncTest {

	@Autowired
	AsyncService asyncService;
	@Autowired
	RestTemplate restTemplate;

	/**
	 * 测试被 @Async 注解的方法，该方法返回 Future 对象
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testReturnedAsync() throws ExecutionException, InterruptedException {
		System.out.println(Thread.currentThread().getName() + " 主线程正在执行......");
		CompletableFuture<Integer> completableFuture = this.asyncService.async2();
		completableFuture.get();
	}

	/**
	 * 同一个类中，异步方法调用异步方法
	 */
	@Test
	public void testAsyncInvokeAsync() throws InterruptedException {
		System.out.println(Thread.currentThread().getName() + " 主线程正在执行......");
		this.asyncService.async1();
		TimeUnit.SECONDS.sleep(30);
	}
}

package org.springframework.demo.util.concurrent.async;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Jie Zhao
 * @date 2024/6/15 9:56
 */
@Service
public class AsyncService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);
	protected final Log logger = LogFactory.getLog(getClass());

	@Async
	public void async1() {
		try {
			System.out.println(Thread.currentThread().getName() + " 异步任务1正在执行......");
			CompletableFuture<Integer> completableFuture = async2();
			completableFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Async
	public CompletableFuture<Integer> async2() {
		System.out.println(Thread.currentThread().getName() + " 异步任务2正在执行......");
		return CompletableFuture.supplyAsync(() -> {
			try {
				System.out.println(Thread.currentThread().getName() + " 异步任务2正在执行......");
//				log.info("异步任务2正在执行......");
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
//			log.info("异步任务2执行完成......");
			System.out.println(Thread.currentThread().getName() + " 异步任务2执行完成......");

			return 10;
		});
	}
}

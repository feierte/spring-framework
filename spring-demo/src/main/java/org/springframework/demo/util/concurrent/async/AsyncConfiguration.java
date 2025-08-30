package org.springframework.demo.util.concurrent.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * @author Jie Zhao
 * @date 2024/6/15 9:52
 */
@Configuration
@ComponentScan(basePackages = {"org.springframework.demo.concurrent.scheduling.async"})
@EnableAsync
public class AsyncConfiguration {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setQueueCapacity(100);
		taskExecutor.setKeepAliveSeconds(60);
		taskExecutor.setThreadNamePrefix("async-pool-");
		return taskExecutor;
	}
}

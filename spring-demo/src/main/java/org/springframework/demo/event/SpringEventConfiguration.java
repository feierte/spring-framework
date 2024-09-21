package org.springframework.demo.event;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Jie Zhao
 * @date 2024/9/21 9:03
 */
@Configuration
@EnableAsync // 在这里开启异步，则会以异步方式执行事件
@ComponentScan(basePackages = {"org.springframework.demo.event"})
public class SpringEventConfiguration {
}

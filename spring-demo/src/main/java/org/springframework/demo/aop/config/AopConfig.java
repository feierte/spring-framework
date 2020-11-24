package org.springframework.demo.aop.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author Jie Zhao
 * @date 2020/11/24 20:38
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.demo.aop")
@EnableAspectJAutoProxy(proxyTargetClass = true) // 开启 Spring注解aop配置 的支持
public class AopConfig {


}

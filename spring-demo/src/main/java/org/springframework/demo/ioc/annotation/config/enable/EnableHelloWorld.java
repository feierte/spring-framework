package org.springframework.demo.ioc.annotation.config.enable;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Jie Zhao
 * @date 2021/2/5 11:12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HelloWorldConfiguration.class) // 第二步：通过Import注解导入具体实现
public @interface EnableHelloWorld { // 第一步：通过@EnableXXX来命名
}

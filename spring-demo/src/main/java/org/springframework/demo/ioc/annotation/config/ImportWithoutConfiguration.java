package org.springframework.demo.ioc.annotation.config;

import org.springframework.context.annotation.Import;

/**
 * 测试@Import能不能单独使用，即不和@Configuration注解一起使用
 * @author Jie Zhao
 * @date 2021/1/24 15:36
 */
@Import(MyImportSelector.class)
public class ImportWithoutConfiguration {
}

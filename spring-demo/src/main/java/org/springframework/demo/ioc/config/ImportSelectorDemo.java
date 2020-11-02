package org.springframework.demo.ioc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Jie Zhao
 * @date 2020/11/2 19:33
 */
@Configuration
@ComponentScan(basePackages = {"org.springframework.demo.ioc.service.impl"})
@Import(MyImportSelector.class)
public class ImportSelectorDemo {
}

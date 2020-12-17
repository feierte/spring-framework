package org.springframework.demo.ioc.beanfactory.di;

/**
 * @author Jie Zhao
 * @date 2020/12/17 21:51
 */

import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectedUser {
}

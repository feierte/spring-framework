package org.springframework.demo.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Jie Zhao
 * @date 2020/11/30 19:58
 */
@Controller
public class HelloController {

    @RequestMapping("/hello")
    public String sayHello() {
        System.out.println("控制器方法执行了");
        return "hello";
    }
}

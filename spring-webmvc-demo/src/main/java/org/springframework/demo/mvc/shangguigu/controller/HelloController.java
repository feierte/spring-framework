package org.springframework.demo.mvc.shangguigu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jie Zhao
 * @date 2022/11/22 23:29
 */
@Controller
@ResponseBody
public class HelloController {

	@RequestMapping("/hello")
	public String hello(String name, int age) {
		return "Hello World!" + name + age;
	}
}

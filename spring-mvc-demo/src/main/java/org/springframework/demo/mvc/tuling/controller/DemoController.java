package org.springframework.demo.mvc.tuling.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jie Zhao
 * @date 2021/10/27 15:14
 */
@Controller
@Slf4j
public class DemoController {

	@RequestMapping("/hello")
	@ResponseBody
	public String sayHello(String name) {
		System.out.println("DemoController控制器执行了...Hello " + name);
		// log.info("DemoController控制器执行了...Hello {}", name);
		return "success";
	}
}

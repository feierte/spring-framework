package org.springframework.demo.util.metadata.annotation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jie Zhao
 * @date 2021/11/9 11:41
 */
@RestController("/test")
public class ControllerDemo {

	@PostMapping("/home")
	public String hello() {
		return "Hello World!!!";
	}
}

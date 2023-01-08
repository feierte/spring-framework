package org.springframework.demo.environment;

import org.springframework.core.env.StandardEnvironment;

/**
 * @author jie zhao
 * @date 2022/12/31 20:34
 */
public class StandardEnvironmentDemo {

	public static void main(String[] args) {
		StandardEnvironment environment = new StandardEnvironment();

		System.out.println(environment.getActiveProfiles());
		System.out.println(environment.getProperty("user.name"));
	}
}

package org.springframework.demo.env;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.Assert;


import java.util.Collections;

/**
 * @author Jie Zhao
 * @date 2024/9/15 10:28
 */
public class PropertySourceTest {

	/**
	 *
	 * @see {@link PropertySource#getProperty(String)}
	 * @see {@link MapPropertySource#getProperty(String)}
	 */
	@Test
	public void testGetProperty() {
		PropertySource mapPropertySource = new MapPropertySource("map",
				Collections.singletonMap("key", "source1"));
		// todo: 这里是 junit 中的
		Assert.assertEquals("value1", mapPropertySource.getProperty("key"));

		ResourcePropertySource resourcePropertySource = new ResourcePropertySource(
				"resource", "classpath:resources.properties");
		Assert.assertEquals("value2", resourcePropertySource.getProperty("key"));
	}
}

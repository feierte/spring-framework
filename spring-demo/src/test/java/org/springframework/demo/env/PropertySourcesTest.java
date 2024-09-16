package org.springframework.demo.env;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

import java.util.Collections;

/**
 * @author Jie Zhao
 * @date 2024/9/15 10:32
 */
public class PropertySourcesTest {

	/**
	 * @see {@link org.springframework.core.env.PropertySources#get(String)}
	 * @see {@link PropertySource#getProperty(String)}
	 */
	@Test
	public void testGetProperty() {
		PropertySource propertySource1 = new MapPropertySource("source1",
				Collections.singletonMap("key", "value1"));
		PropertySource propertySource2 = new MapPropertySource("source2",
				Collections.singletonMap("key", "value2"));

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource1);
		propertySources.addLast(propertySource2);

		// todo: 这里是 junit 中的
		Assert.assertEquals("value1", propertySources.get("source1").getProperty("key"));
		Assert.assertEquals("value2", propertySources.get("source2").getProperty("key"));
	}
}

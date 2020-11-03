package org.springframework.demo.ioc.annotation.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

/**
 * @author Jie Zhao
 * @date 2020/11/2 21:44
 */
@Configuration
@org.springframework.context.annotation.PropertySource(value = "classpath:jdbc.yml",
		factory = YamlPropertySourceFactoryDemo.class)
@Import(JdbcConfig.class)
public class YamlPropertySourceFactoryDemo implements PropertySourceFactory {


	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {

		// 创建yaml文件解析工厂
		YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
		// 设置要解析的资源内容
		factoryBean.setResources(resource.getResource());
		// 把资源解析城Properties文件
		Properties properties = factoryBean.getObject();
		// 返回PropertySource对象
		return (name != null ? new PropertiesPropertySource(name, properties)
				: new PropertiesPropertySource(resource.getResource().getFilename(), properties));
	}


	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.demo");
		// applicationContext.register(YamlPropertySourceFactoryDemo.class);
		DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
		Connection connection = dataSource.getConnection();
		connection.close();
	}
}

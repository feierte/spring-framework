package org.springframework.demo.ioc.annotation.metadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;

@Repository("repositoryName")
@Service("serviceName")
@EnableAsync
public class Metadata extends HashMap<String, String> implements Serializable {

	private static class InnerClass {

	}

	@Autowired
	private String getName() {
		return "demo";
	}
}
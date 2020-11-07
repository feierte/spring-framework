package org.springframework.demo.ioc.metadata;

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


	private static final long serialVersionUID = -2805058363835019969L;


	private static class InnerClass {

	}

	@Autowired
	private String getName() {
		return "demo";
	}
}
package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author jie zhao
 * @date 2020/5/1 14:42
 */

/*@Data
@ToString*/
public class User implements BeanNameAware {

    private int id;

    private String name;

    private int age;

	private City city;

	private City[] workCities;

	private List<City> lifeCities;

	private Resource configFileLocation;

	private Company company;

	private Properties context;

	private String contextAsText;

	private String beanName;

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

    public User() {

    }

    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public User(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public static User createUser() {
		User user = new User();
		user.setId(1);
		user.setName("xiaomage");
		return user;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

	public City[] getWorkCities() {
		return workCities;
	}

	public void setWorkCities(City[] workCities) {
		this.workCities = workCities;
	}

	public List<City> getLifeCities() {
		return lifeCities;
	}

	public void setLifeCities(List<City> lifeCities) {
		this.lifeCities = lifeCities;
	}

	public Resource getConfigFileLocation() {
		return configFileLocation;
	}

	public void setConfigFileLocation(Resource configFileLocation) {
		this.configFileLocation = configFileLocation;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Properties getContext() {
		return context;
	}

	public void setContext(Properties context) {
		this.context = context;
	}

	public String getContextAsText() {
		return contextAsText;
	}

	public void setContextAsText(String contextAsText) {
		this.contextAsText = contextAsText;
	}

	@Override
	public String toString() {
		return "User{" +
				"id=" + id +
				", name='" + name + '\'' +
				", city=" + city +
				", workCities=" + Arrays.toString(workCities) +
				", lifeCities=" + lifeCities +
				", configFileLocation=" + configFileLocation +
				", company=" + company +
				", context=" + context +
				", contextAsText='" + contextAsText + '\'' +
				", beanName='" + beanName + '\'' +
				'}';
	}


}

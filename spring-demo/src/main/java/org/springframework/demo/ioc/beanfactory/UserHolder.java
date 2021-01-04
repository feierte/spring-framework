package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class UserHolder implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware,
        InitializingBean, SmartInitializingSingleton, DisposableBean {

    private User user;

    private String description;

    private String beanName;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

	private Integer number;

    @PostConstruct
    public void initPostConstruct() {
        this.description = "the description of userholder V1.";
        System.out.println("initPostConstruct(): " + description);
    }

	@PreDestroy
	public void preDestroy() {
		// postProcessBeforeDestruction : The user holder V9
		this.description = "The user holder V10";
		System.out.println("preDestroy() = " + description);
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        this.description = "the description of userholder V2.";
        System.out.println("afterPropertiesSet(): " + description);
    }

    public void init() {
        this.description = "the description of userholder V3.";
        System.out.println("init(): " + description);
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.description = "the description of userholder V4.";
        System.out.println("afterSingletonsInstantiated(): " + description);
    }

	public void doDestroy() {
		// destroy : The user holder V11
		this.description = "The user holder V12";
		System.out.println("doDestroy() = " + description);
	}

	public UserHolder() {
	}

	public UserHolder(User user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	@Override
	public String toString() {
		return "UserHolder{" +
				"user=" + user +
				", description='" + description + '\'' +
				", beanName='" + beanName + '\'' +
				", classLoader=" + classLoader +
				", beanFactory=" + beanFactory +
				", number=" + number +
				'}';
	}

	@Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void destroy() throws Exception {
		// preDestroy : The user holder V10
		this.description = "The user holder V11";
		System.out.println("destroy() = " + description);
	}

	protected void finalize() throws Throwable {
		System.out.println("The UserHolder is finalized...");
	}
}

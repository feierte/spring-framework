package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;

import javax.annotation.PostConstruct;

public class UserHolder implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware,
        InitializingBean, SmartInitializingSingleton {

    private User user;

    private String description;

    private String beanName;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

    @PostConstruct
    public void initPostConstruct() {
        this.description = "the description of userholder V1.";
        System.out.println("initPostConstruct(): " + description);
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

    @Override
    public String toString() {
        return "UserHolder{" +
                "user=" + user +
                ", description='" + description + '\'' +
                ", beanName='" + beanName + '\'' +
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

}

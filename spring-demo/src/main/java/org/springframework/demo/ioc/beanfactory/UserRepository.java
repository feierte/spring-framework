package org.springframework.demo.ioc.beanfactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * @author Jie Zhao
 * @date 2020/12/5 12:02
 */
/*@Data*/
public class UserRepository {

	/**
	 * 注入自定义bean
	 */
	private Collection<User> users;

	/**
	 * 注入Spring内建非bean对象 （依赖）
	 */
	private BeanFactory beanFactory;


	/**
	 * 延迟注入
	 */
	private ObjectFactory<User> userObjectFactory;

	/**
	 * 延迟注入
	 */
	private ObjectFactory<ApplicationContext> objectFactory;

	public Collection<User> getUsers() {
		return users;
	}

	public void setUsers(Collection<User> users) {
		this.users = users;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public ObjectFactory<User> getUserObjectFactory() {
		return userObjectFactory;
	}

	public void setUserObjectFactory(ObjectFactory<User> userObjectFactory) {
		this.userObjectFactory = userObjectFactory;
	}

	public ObjectFactory<ApplicationContext> getObjectFactory() {
		return objectFactory;
	}

	public void setObjectFactory(ObjectFactory<ApplicationContext> objectFactory) {
		this.objectFactory = objectFactory;
	}

	@Override
	public String toString() {
		return "UserRepository{" +
				"users=" + users +
				'}';
	}
}

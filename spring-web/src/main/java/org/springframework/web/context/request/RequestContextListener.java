/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Servlet listener that exposes the request to the current thread,
 * through both {@link org.springframework.context.i18n.LocaleContextHolder} and
 * {@link RequestContextHolder}. To be registered as listener in {@code web.xml}.
 *
 * <p>Alternatively, Spring's {@link org.springframework.web.filter.RequestContextFilter}
 * and Spring's {@link org.springframework.web.servlet.DispatcherServlet} also expose
 * the same request context to the current thread. In contrast to this listener,
 * advanced options are available there (e.g. "threadContextInheritable").
 *
 * <p>This listener is mainly for use with third-party servlets, e.g. the JSF FacesServlet.
 * Within Spring's own web support, DispatcherServlet's processing is perfectly sufficient.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.servlet.ServletRequestListener
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see RequestContextHolder
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextListener implements ServletRequestListener {

	private static final String REQUEST_ATTRIBUTES_ATTRIBUTE =
			RequestContextListener.class.getName() + ".REQUEST_ATTRIBUTES";


	// ServletRequest 创建的时候会触发该方法被调用执行
	@Override
	public void requestInitialized(ServletRequestEvent requestEvent) {
		// 判断是否是 HttpServletRequest（即目前只支持 http 请求）
		if (!(requestEvent.getServletRequest() instanceof HttpServletRequest)) {
			throw new IllegalArgumentException(
					"Request is not an HttpServletRequest: " + requestEvent.getServletRequest());
		}
		// 获取当前的 request
		HttpServletRequest request = (HttpServletRequest) requestEvent.getServletRequest();
		// 创建  ServletRequestAttributes 对象
		// 该对象只是简单的封装了 request，实际上 scope 为 request 或 session 的 Bean 都存储在 request 或 session 中
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
		LocaleContextHolder.setLocale(request.getLocale());
		// 将 ServletRequestAttributes 与当前线程绑定，因为是一个静态方法，因此这意味着无论在哪里，只要调用
		// RequestContextHolder.getRequestAttributes().getAttribute().setAttribute()
		// 都能将 Bean 和 request 进行绑定，而且在一次 request 的生命周期中不会重复创建对象
		RequestContextHolder.setRequestAttributes(attributes);
	}

	// ServletRequest 销毁的时候会触发该方法被调用执行
	@Override
	public void requestDestroyed(ServletRequestEvent requestEvent) {
		ServletRequestAttributes attributes = null;
		Object reqAttr = requestEvent.getServletRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);
		if (reqAttr instanceof ServletRequestAttributes) {
			attributes = (ServletRequestAttributes) reqAttr;
		}
		RequestAttributes threadAttributes = RequestContextHolder.getRequestAttributes();
		if (threadAttributes != null) {
			// We're assumably within the original request thread...
			LocaleContextHolder.resetLocaleContext();
			RequestContextHolder.resetRequestAttributes();
			if (attributes == null && threadAttributes instanceof ServletRequestAttributes) {
				attributes = (ServletRequestAttributes) threadAttributes;
			}
		}
		if (attributes != null) {
			attributes.requestCompleted();
		}
	}

}

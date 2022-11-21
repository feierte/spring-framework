package org.springframework.demo.tomcat.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 该 Servlet 用于测试 ServletContainerInitializer
 * @author Jie Zhao
 * @date 2021/11/1 11:00
 *
 * @see javax.servlet.ServletContainerInitializer
 */
public class ServletContainerInitializerServlet extends HttpServlet {


	private static final long serialVersionUID = -4130545586143906542L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().println("ServletContainerInitializerServlet executed...");
	}
}

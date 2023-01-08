package org.springframework.demo.web;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Jie Zhao
 * @date 2021/9/14 21:18
 */
public class RestTemplateTest {

	public static void main(String[] args) {
		RestTemplate restTemplate = new RestTemplate();
		/*restTemplate.setInterceptors(Arrays.asList(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
				System.out.println("执行拦截器，HttpRequest: " + request);
				return null;
			}
		}));*/
		String body = restTemplate.getForEntity("http://www.baidu.com", String.class).getBody();
		System.out.println(body);
	}
}

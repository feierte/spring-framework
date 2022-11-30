package org.springframework.demo.mvc.shangguigu;

/**
 * @author jie zhao
 * @date 2022/11/29 22:26
 */
public class ResponseData {

	private Object data;
	private int status;

	public ResponseData() {

	}

	public ResponseData(Object data, int status) {
		this.data = data;
		this.status = status;
	}

	public static ResponseData success(Object data) {
		return new ResponseData(data, 200);
	}

	public static ResponseData error(Object data) {
		return new ResponseData(data, 500);
	}
}

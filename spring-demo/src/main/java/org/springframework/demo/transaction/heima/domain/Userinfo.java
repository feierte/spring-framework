package org.springframework.demo.transaction.heima.domain;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Jie Zhao
 * @date 2021/10/8 10:53
 */
public class Userinfo implements Serializable {

	private static final long serialVersionUID = 6858236276707303283L;
	private Integer id;
	private byte[] images;
	private String description;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public byte[] getImages() {
		return images;
	}

	public void setImages(byte[] images) {
		this.images = images;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "Userinfo{" +
				"id=" + id +
				", images=" + Arrays.toString(images) +
				", description='" + description + '\'' +
				'}';
	}
}
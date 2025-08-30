package org.springframework.demo.util.metadata.annotation;

import java.util.Arrays;
import java.util.List;

/**
 * @author Jie Zhao
 * @date 2023/1/30 22:04
 */
@StringRepository("chineseNameRepository")
public class NameRepository {

	/**
	 * 查找所有的名字
	 * @return
	 */
	public List<String> findAll() {
		return Arrays.asList("张三", "李四", "小马哥");
	}
}

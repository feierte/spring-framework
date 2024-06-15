package org.springframework.demo.transaction.heima;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.demo.transaction.heima.config.SpringConfiguration;
import org.springframework.demo.transaction.heima.domain.Account;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2021/10/8 10:52
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfiguration.class)
public class SpringNamedParameterJdbcTemplateTest {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	public void testFind() {
		Map<String, Object> map = new HashMap<>();
		map.put("id", 1);
		Account account = jdbcTemplate.queryForObject("select * from account where id = :id", map, new BeanPropertyRowMapper<Account>(Account.class));
		System.out.println(account);
	}


	@Test
	public void testSave() {
		Account account = new Account();
		account.setName("NamedParameterJdbcTemplate");
		account.setMoney(12345d);
		BeanMap beanMap = BeanMap.create(account);
		jdbcTemplate.update("insert into account(name,money)values(:name,:money)", beanMap);
	}
}

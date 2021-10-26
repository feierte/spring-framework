package org.springframework.demo.transaction.heima;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.demo.transaction.heima.config.JdbcConfig;
import org.springframework.demo.transaction.heima.config.SpringConfiguration;
import org.springframework.demo.transaction.heima.domain.Account;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2021/10/20 10:09
 */
@Component
public class SpringJdbcTemplateTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void testJdbcTemplate() {
		System.out.println(jdbcTemplate);
	}

	public void testSave() {
		jdbcTemplate.update("insert into account(money,name)values(?,?)", 6789d, "userTest");
	}

	public void testUpdate() {
		jdbcTemplate.update("update account set name=?,money=? where id=?", "testZZZ", 23456d, 3);
	}

	public void testDelete() {
		jdbcTemplate.update("delete from account where id = ? ", 4);
	}

	public void testFindOne() {
//        List<Account> accounts =  jdbcTemplate.query("select * from account where id = ?",new BeanPropertyRowMapper<Account>(Account.class),1);
//        System.out.println(accounts.isEmpty()?"empty":accounts.get(0));

//        Account account = jdbcTemplate.queryForObject("select * from account where id = ?",new BeanPropertyRowMapper<Account>(Account.class),1);
//        System.out.println(account);

		Account account = jdbcTemplate.query("select * from account where id = ?", new ResultSetExtractor<Account>() {
			@Override
			public Account extractData(ResultSet rs) throws SQLException, DataAccessException {
				Account account1 = null;
				//1.判断结果集能往下走
				if (rs.next()) {
					account1 = new Account();
					account1.setId(rs.getInt("id"));
					account1.setName(rs.getString("name"));
					account1.setMoney(rs.getDouble("money"));
				}
				return account1;
			}
		}, 1);
		System.out.println(account);
	}

	public void testFindAll() {
		List<Account> accountList = jdbcTemplate.query("select * from account where money > ?", new BeanPropertyRowMapper<Account>(Account.class), 999d);
		for (Account account : accountList) {
			System.out.println(account);
		}
	}

	public void testFindCount() {
		Integer count = jdbcTemplate.queryForObject("select count(*) from account where money > ?", Integer.class, 999d);
		System.out.println(count);
	}


	public void testQueryForList() {
		/**
		 * 得到某个特定类型的集合。类型是方法的第二个参数指定的
		 */
		List<Double> list = jdbcTemplate.queryForList("select money from account where money > ?", Double.class, 999d);
		for (Double money : list) {
			System.out.println(money);
		}

//        List<Map<String,Object>> list = jdbcTemplate.queryForList("select * from account where money > ? ",999d);
//        for(Map<String,Object> map : list){
//            for(Map.Entry<String,Object> me : map.entrySet())
//            System.out.println(me.getKey()+","+me.getValue());
//        }
	}

	public void testQueryForMap() {
		Map<String, Object> map = jdbcTemplate.queryForMap("select * from account where id = ?", 1);
		for (Map.Entry<String, Object> me : map.entrySet()) {
			System.out.println(me.getKey() + "," + me.getValue());
		}
	}


	public void testQueryForRowSet() {
		SqlRowSet rowSet = jdbcTemplate.queryForRowSet("select * from account where money > ?", 999d);
		System.out.println(rowSet);
		while (rowSet.next()) {
			String name = rowSet.getString("name");
			System.out.println(name);
		}
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
		SpringJdbcTemplateTest springJdbcTemplateTest = applicationContext.getBean("springJdbcTemplateTest", SpringJdbcTemplateTest.class);
		springJdbcTemplateTest.testJdbcTemplate();
	}
}

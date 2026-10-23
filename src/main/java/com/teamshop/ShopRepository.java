package com.teamshop;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;
@Repository
public class ShopRepository {
 final JdbcTemplate db;
 public ShopRepository(JdbcTemplate db){this.db=db;}
 public JdbcTemplate jdbc(){return db;}
 public long insert(String sql,Object...args){
  KeyHolder key=new GeneratedKeyHolder(); db.update(c->{var p=c.prepareStatement(sql,new String[]{"id"});for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},key);return key.getKey().longValue();
 }
 public Map<String,Object> one(String sql,Object...args){return db.queryForList(sql,args).stream().findFirst().orElseThrow(()->new IllegalArgumentException("대상을 찾을 수 없습니다."));}
 public long member(String email){return ((Number)one("SELECT id FROM members WHERE email=?",email).get("id")).longValue();}
 public List<Map<String,Object>> products(String q,String cat,int page){return db.queryForList("SELECT * FROM products WHERE active=TRUE AND LOWER(name) LIKE ? AND (?='' OR category=?) ORDER BY id DESC LIMIT 8 OFFSET ?", "%"+q.toLowerCase()+"%",cat,cat,page*8);}
 public long count(String q,String cat){return db.queryForObject("SELECT COUNT(*) FROM products WHERE active=TRUE AND LOWER(name) LIKE ? AND (?='' OR category=?)",Long.class,"%"+q.toLowerCase()+"%",cat,cat);}
 public List<Map<String,Object>> cart(long m){return db.queryForList("SELECT c.id,c.product_id,c.quantity,p.name,p.price,p.stock,p.active FROM cart_items c JOIN products p ON p.id=c.product_id WHERE member_id=? ORDER BY p.id",m);}
 public static long num(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
}

package com.teamshop;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import static com.teamshop.ShopRepository.num;
@Service
public class ShopService {
 final ShopRepository r; final PasswordEncoder encoder;
 public ShopService(ShopRepository r,PasswordEncoder encoder){this.r=r;this.encoder=encoder;}
 @Transactional public void signup(String email,String password,String name){
  if(r.jdbc().queryForObject("SELECT COUNT(*) FROM members WHERE email=?",Long.class,email)>0)throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
  r.jdbc().update("INSERT INTO members(email,password,name,role) VALUES (?,?,?,'USER')",email,encoder.encode(password),name);
 }
 void lockMember(long m){r.one("SELECT id FROM members WHERE id=? FOR UPDATE",m);}
 @Transactional public void add(long m,long p,int qty){
  lockMember(m); if(qty<1||qty>99)throw new IllegalArgumentException("수량은 1~99개입니다.");
  var product=r.one("SELECT * FROM products WHERE id=? AND active=TRUE",p);
  var old=r.jdbc().queryForList("SELECT quantity FROM cart_items WHERE member_id=? AND product_id=?",m,p);
  long n=qty+(old.isEmpty()?0:num(old.get(0),"quantity"));
  if(n>99||n>num(product,"stock"))throw new IllegalArgumentException("재고 또는 최대 수량을 초과했습니다.");
  if(old.isEmpty())r.jdbc().update("INSERT INTO cart_items(member_id,product_id,quantity) VALUES(?,?,?)",m,p,qty);
  else r.jdbc().update("UPDATE cart_items SET quantity=? WHERE member_id=? AND product_id=?",n,m,p);
 }
 @Transactional public void quantity(long m,long id,int qty){
  lockMember(m); if(qty<1||qty>99)throw new IllegalArgumentException("수량은 1~99개입니다.");
  var c=r.one("SELECT p.stock,p.active FROM cart_items c JOIN products p ON p.id=c.product_id WHERE c.id=? AND c.member_id=?",id,m);
  if(!Boolean.TRUE.equals(c.get("active"))||qty>num(c,"stock"))throw new IllegalArgumentException("상품 판매 상태 또는 재고를 확인하세요.");
  r.jdbc().update("UPDATE cart_items SET quantity=? WHERE id=? AND member_id=?",qty,id,m);
 }
 @Transactional public void remove(long m,long id){lockMember(m);r.jdbc().update("DELETE FROM cart_items WHERE id=? AND member_id=?",id,m);}
 @Transactional public long checkout(long m,String recipient,String address){
  lockMember(m);var cart=r.cart(m);if(cart.isEmpty())throw new IllegalArgumentException("장바구니가 비어 있습니다.");
  long total=0;
  for(var c:cart){long qty=num(c,"quantity");
   var p=r.one("SELECT * FROM products WHERE id=? FOR UPDATE",num(c,"product_id"));
   if(!Boolean.TRUE.equals(p.get("active"))||num(p,"stock")<qty)throw new IllegalArgumentException("판매 중지 또는 재고 부족 상품이 있습니다.");
   c.put("price",p.get("price"));c.put("name",p.get("name"));total+=num(p,"price")*qty;
   r.jdbc().update("UPDATE products SET stock=stock-? WHERE id=?",qty,num(p,"id"));
  }
  long order=r.insert("INSERT INTO shop_orders(member_id,recipient,address,total,status) VALUES(?,?,?,?,'PLACED')",m,recipient,address,total);
  for(var c:cart)r.jdbc().update("INSERT INTO order_items(order_id,product_id,product_name,unit_price,quantity) VALUES(?,?,?,?,?)",order,num(c,"product_id"),c.get("name"),num(c,"price"),num(c,"quantity"));
  r.jdbc().update("DELETE FROM cart_items WHERE member_id=?",m);return order;
 }
 @Transactional public void cancel(long m,long id){
  lockMember(m);var order=r.one("SELECT * FROM shop_orders WHERE id=? AND member_id=? FOR UPDATE",id,m);
  if(!"PLACED".equals(order.get("status")))throw new IllegalArgumentException("이미 취소된 주문입니다.");
  for(var i:r.jdbc().queryForList("SELECT * FROM order_items WHERE order_id=? ORDER BY product_id",id))r.jdbc().update("UPDATE products SET stock=stock+? WHERE id=?",num(i,"quantity"),num(i,"product_id"));
  r.jdbc().update("UPDATE shop_orders SET status='CANCELLED' WHERE id=?",id);
 }
}

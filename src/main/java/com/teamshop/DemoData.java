package com.teamshop;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component @Profile("!mysql")
public class DemoData implements CommandLineRunner {
 final ShopRepository r; final PasswordEncoder e;
 public DemoData(ShopRepository r,PasswordEncoder e){this.r=r;this.e=e;}
 public void run(String...args){
  if(r.jdbc().queryForObject("SELECT COUNT(*) FROM members",Long.class)==0){r.jdbc().update("INSERT INTO members(email,password,name,role) VALUES(?,?,?,'ADMIN')","admin@teamshop.local",e.encode("Admin1234!"),"관리자");r.jdbc().update("INSERT INTO members(email,password,name,role) VALUES(?,?,?,'USER')","demo@teamshop.local",e.encode("Demo1234!"),"방문자");}
  if(r.jdbc().queryForObject("SELECT COUNT(*) FROM products",Long.class)==0){String[] names={"세라믹 머그","코튼 핸드타월","데스크 노트","우드 트레이","유리 물병","패브릭 파우치","데일리 플래너","리넨 쿠션","미니 수납함","스테인리스 텀블러"};String[] cats={"주방","리빙","문구"};for(int i=0;i<names.length;i++)r.jdbc().update("INSERT INTO products(name,category,description,price,stock) VALUES(?,?,?,?,?)",names[i],cats[i%3],"매일 곁에 두고 사용하기 좋은 "+names[i]+"입니다. 기본에 충실한 소재와 편안한 색감으로 일상 공간을 채워 보세요.",8000+i*2500,20+i);}
 }
}

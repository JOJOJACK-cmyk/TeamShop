package com.teamshop;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(JdbcTemplate db){return email -> db.query("SELECT * FROM members WHERE email=?", (rs,n)->User.withUsername(rs.getString("email")).password(rs.getString("password")).roles(rs.getString("role")).build(),email).stream().findFirst().orElseThrow(()->new UsernameNotFoundException("회원이 없습니다"));}
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a.requestMatchers("/","/products/**","/login","/signup","/style.css","/error").permitAll().requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",true).permitAll()).logout(l->l.logoutSuccessUrl("/")).build();
 }
}

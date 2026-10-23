package com.teamshop;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ShopIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ShopService service; @Autowired ShopRepository r;
 @BeforeEach void reset(){r.jdbc().update("DELETE FROM order_items");r.jdbc().update("DELETE FROM shop_orders");r.jdbc().update("DELETE FROM cart_items");r.jdbc().update("UPDATE products SET stock=20,active=TRUE");r.jdbc().update("DELETE FROM members WHERE email='new@example.com'");}
 long member(){return r.member("demo@teamshop.local");}
 @Test void publicViewsRender() throws Exception {mvc.perform(get("/").param("q","머그")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("세라믹 머그")));mvc.perform(get("/products/1")).andExpect(status().isOk());mvc.perform(get("/signup")).andExpect(status().isOk());mvc.perform(get("/login")).andExpect(status().isOk());}
 @Test void rejectsCsrfAndNonAdmin() throws Exception {mvc.perform(post("/cart/add").with(user("demo@teamshop.local")).param("productId","1")).andExpect(status().isForbidden());mvc.perform(get("/admin/products").with(user("demo@teamshop.local"))).andExpect(status().isForbidden());mvc.perform(get("/cart")).andExpect(status().is3xxRedirection());}
 @Test void checkoutUsesServerPriceAndCancelRestoresStock(){long m=member();int before=r.jdbc().queryForObject("SELECT stock FROM products WHERE id=1",Integer.class);service.add(m,1,2);long id=service.checkout(m,"테스터","서울시 테스트 주소");assertThat(r.jdbc().queryForObject("SELECT total FROM shop_orders WHERE id=?",Long.class,id)).isEqualTo(16000);assertThat(r.cart(m)).isEmpty();assertThat(r.jdbc().queryForObject("SELECT stock FROM products WHERE id=1",Integer.class)).isEqualTo(before-2);service.cancel(m,id);assertThat(r.jdbc().queryForObject("SELECT stock FROM products WHERE id=1",Integer.class)).isEqualTo(before);assertThatThrownBy(()->service.cancel(m,id)).isInstanceOf(IllegalArgumentException.class);}
 @Test void insufficientStockRollsBackAllItems(){long m=member();service.add(m,1,2);service.add(m,2,1);int before=r.jdbc().queryForObject("SELECT stock FROM products WHERE id=1",Integer.class);r.jdbc().update("UPDATE products SET stock=0 WHERE id=2");assertThatThrownBy(()->service.checkout(m,"테스터","서울시")).isInstanceOf(IllegalArgumentException.class);assertThat(r.jdbc().queryForObject("SELECT stock FROM products WHERE id=1",Integer.class)).isEqualTo(before);assertThat(r.jdbc().queryForObject("SELECT COUNT(*) FROM shop_orders",Long.class)).isZero();assertThat(r.cart(m)).hasSize(2);}
 @Test void cannotReadOrCancelAnotherMembersOrder() throws Exception {long id=serviceOrder();mvc.perform(get("/orders/"+id).with(user("admin@teamshop.local").roles("ADMIN"))).andExpect(status().isBadRequest());assertThatThrownBy(()->service.cancel(r.member("admin@teamshop.local"),id)).isInstanceOf(IllegalArgumentException.class);}
 long serviceOrder(){service.add(member(),1,1);return service.checkout(member(),"테스터","서울");}
 @Test void userPagesAndAdminFormsRender() throws Exception {service.add(member(),1,1);mvc.perform(get("/cart").with(user("demo@teamshop.local"))).andExpect(status().isOk());long id=service.checkout(member(),"테스터","서울");mvc.perform(get("/orders").with(user("demo@teamshop.local"))).andExpect(status().isOk());mvc.perform(get("/orders/"+id).with(user("demo@teamshop.local"))).andExpect(status().isOk());mvc.perform(get("/admin/products").with(user("admin@teamshop.local").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/admin/products/new").with(user("admin@teamshop.local").roles("ADMIN"))).andExpect(status().isOk());}
 @Test void signupValidatesAndHashesPassword() throws Exception {mvc.perform(post("/signup").with(csrf()).param("email","new@example.com").param("password","abc").param("name","새회원")).andExpect(status().isBadRequest());mvc.perform(post("/signup").with(csrf()).param("email","new@example.com").param("password","Password123!").param("name","새회원")).andExpect(status().is3xxRedirection());assertThat(r.one("SELECT password FROM members WHERE email='new@example.com'").get("password").toString()).startsWith("$2");assertThatThrownBy(()->service.signup("new@example.com","Password123!","중복")).isInstanceOf(IllegalArgumentException.class);}
 @Test void cartBoundsAndSearch() throws Exception {assertThatThrownBy(()->service.add(member(),1,100)).isInstanceOf(IllegalArgumentException.class);assertThat(r.products("머그","주방",0)).hasSize(1);assertThat(r.products("없음","",0)).isEmpty();mvc.perform(get("/?page=1")).andExpect(status().isOk());}
}

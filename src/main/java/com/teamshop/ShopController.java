package com.teamshop;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import static com.teamshop.ShopRepository.num;
@Controller
public class ShopController {
 final ShopRepository r; final ShopService s;
 public ShopController(ShopRepository r,ShopService s){this.r=r;this.s=s;}
 public record Signup(@Email @NotBlank @Size(max=150) String email,@NotBlank @Size(min=8,max=72) String password,@NotBlank @Size(max=40) String name){}
 public record Shipping(@NotBlank @Size(max=40) String recipient,@NotBlank @Size(max=250) String address){}
 public record Product(@NotBlank @Size(max=100) String name,@Pattern(regexp="주방|리빙|문구") String category,@NotBlank @Size(max=1500) String description,@Min(1) @Max(10000000) int price,@Min(0) @Max(1000000) int stock){}
 @ModelAttribute void common(Principal p,Model model){model.addAttribute("signedIn",p!=null);model.addAttribute("isAdmin",p!=null&&"ADMIN".equals(r.one("SELECT role FROM members WHERE email=?",p.getName()).get("role")));}
 @GetMapping("/") String home(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String category,@RequestParam(defaultValue="0") int page,Model model){page=Math.max(0,Math.min(page,100000));model.addAttribute("products",r.products(q,category,page));model.addAttribute("count",r.count(q,category));model.addAttribute("q",q);model.addAttribute("category",category);model.addAttribute("page",page);return "home";}
 @GetMapping("/products/{id}") String product(@PathVariable long id,Model m){m.addAttribute("p",r.one("SELECT * FROM products WHERE id=? AND active=TRUE",id));return "product";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/signup") String signup(){return "signup";}
 @PostMapping("/signup") String signup(@Valid @ModelAttribute Signup form,RedirectAttributes a){s.signup(form.email().trim().toLowerCase(),form.password(),form.name().trim());a.addFlashAttribute("message","가입 완료! 로그인해 주세요.");return "redirect:/login";}
 @GetMapping("/cart") String cart(Principal p,Model m){var cart=r.cart(r.member(p.getName()));m.addAttribute("items",cart);m.addAttribute("total",cart.stream().mapToLong(c->num(c,"price")*num(c,"quantity")).sum());return "cart";}
 @PostMapping("/cart/add") String add(Principal p,@RequestParam long productId,@RequestParam(defaultValue="1") int quantity){s.add(r.member(p.getName()),productId,quantity);return "redirect:/cart";}
 @PostMapping("/cart/{id}/quantity") String quantity(Principal p,@PathVariable long id,@RequestParam int quantity){s.quantity(r.member(p.getName()),id,quantity);return "redirect:/cart";}
 @PostMapping("/cart/{id}/delete") String remove(Principal p,@PathVariable long id){s.remove(r.member(p.getName()),id);return "redirect:/cart";}
 @PostMapping("/orders") String order(Principal p,@Valid @ModelAttribute Shipping shipping){return "redirect:/orders/"+s.checkout(r.member(p.getName()),shipping.recipient(),shipping.address());}
 @GetMapping("/orders") String orders(Principal p,Model m){m.addAttribute("orders",r.jdbc().queryForList("SELECT * FROM shop_orders WHERE member_id=? ORDER BY id DESC",r.member(p.getName())));return "orders";}
 @GetMapping("/orders/{id}") String order(Principal p,@PathVariable long id,Model m){m.addAttribute("order",r.one("SELECT * FROM shop_orders WHERE id=? AND member_id=?",id,r.member(p.getName())));m.addAttribute("items",r.jdbc().queryForList("SELECT * FROM order_items WHERE order_id=?",id));return "order";}
 @PostMapping("/orders/{id}/cancel") String cancel(Principal p,@PathVariable long id){s.cancel(r.member(p.getName()),id);return "redirect:/orders/"+id;}
 @GetMapping("/admin/products") String admin(Model m){m.addAttribute("products",r.jdbc().queryForList("SELECT * FROM products ORDER BY id DESC"));return "admin";}
 @GetMapping("/admin/products/new") String newProduct(Model m){m.addAttribute("p",Map.of("id",0,"name","","description","","price",1000,"stock",10,"category","리빙"));return "edit";}
 @GetMapping("/admin/products/{id}/edit") String edit(@PathVariable long id,Model m){m.addAttribute("p",r.one("SELECT * FROM products WHERE id=?",id));return "edit";}
 @PostMapping("/admin/products/save") String save(@RequestParam long id,@Valid @ModelAttribute Product form){if(id==0)r.jdbc().update("INSERT INTO products(name,category,description,price,stock) VALUES(?,?,?,?,?)",form.name(),form.category(),form.description(),form.price(),form.stock());else r.jdbc().update("UPDATE products SET name=?,category=?,description=?,price=?,stock=? WHERE id=?",form.name(),form.category(),form.description(),form.price(),form.stock(),id);return "redirect:/admin/products";}
 @PostMapping("/admin/products/{id}/toggle") String toggle(@PathVariable long id){r.jdbc().update("UPDATE products SET active=NOT active WHERE id=?",id);return "redirect:/admin/products";}
}

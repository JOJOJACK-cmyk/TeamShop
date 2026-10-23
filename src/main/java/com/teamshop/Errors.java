package com.teamshop;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.dao.DataIntegrityViolationException;
@ControllerAdvice
public class Errors {
 @ExceptionHandler({IllegalArgumentException.class,BindException.class,DataIntegrityViolationException.class})
 @ResponseStatus(HttpStatus.BAD_REQUEST)
 String bad(Exception e,Model m){m.addAttribute("error",e instanceof IllegalArgumentException?e.getMessage():"입력 값 또는 중복 데이터를 확인해 주세요.");return "problem";}
}

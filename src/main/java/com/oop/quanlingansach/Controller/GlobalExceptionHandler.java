package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Service.BusinessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Lưới an toàn: lỗi nghiệp vụ nào controller không tự xử lý thì đưa người dùng
 * về trang chủ kèm thông báo, thay vì hiện trang lỗi 500.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/";
    }
}

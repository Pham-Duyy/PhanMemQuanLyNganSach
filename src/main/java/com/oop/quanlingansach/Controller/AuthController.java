package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Dto.RegisterForm;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Đăng nhập, đăng ký, đăng xuất và trang thông tin cá nhân (dùng chung cho mọi vai trò).
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Trang chủ: chuyển tới dashboard theo vai trò
    @GetMapping("/")
    public String home(@SessionAttribute(name = SessionKeys.CURRENT_USER, required = false) User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        return dashboardOf(currentUser);
    }

    // ==================== ĐĂNG NHẬP ====================

    @GetMapping("/login")
    public String showLoginForm(@SessionAttribute(name = SessionKeys.CURRENT_USER, required = false) User currentUser) {
        return currentUser != null ? "redirect:/" : "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(defaultValue = "user") String userType,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (username.isBlank() || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập tên đăng nhập và mật khẩu!");
            return "redirect:/login";
        }

        Optional<User> authenticated;
        try {
            authenticated = userService.authenticate(username.trim(), password);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
        if (authenticated.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
            return "redirect:/login";
        }

        // Người dùng chọn đăng nhập với vai trò nào thì tài khoản phải đúng vai trò đó
        User user = authenticated.get();
        boolean wantsAdmin = "admin".equals(userType);
        if (wantsAdmin != user.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", wantsAdmin
                    ? "Tài khoản này không có quyền quản trị viên!"
                    : "Tài khoản này không phải là tài khoản người dùng thường!");
            return "redirect:/login";
        }

        // Cấp session ID mới sau khi đăng nhập để chống chiếm phiên (session fixation)
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(SessionKeys.CURRENT_USER, user);
        redirectAttributes.addFlashAttribute("success", "Chào mừng " + user.getFullName() + "! Đăng nhập thành công.");
        return dashboardOf(user);
    }

    // ==================== ĐĂNG KÝ ====================

    @GetMapping("/auth/register")
    public String showRegisterForm(@SessionAttribute(name = SessionKeys.CURRENT_USER, required = false) User currentUser,
                                   Model model) {
        if (currentUser != null) {
            return "redirect:/";
        }
        model.addAttribute("user", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@ModelAttribute("user") RegisterForm form, RedirectAttributes redirectAttributes) {
        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập để tiếp tục.");
            return "redirect:/login";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    // ==================== ĐĂNG XUẤT ====================

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đăng xuất thành công!");
        return "redirect:/login";
    }

    // ==================== THÔNG TIN CÁ NHÂN ====================

    @GetMapping("/auth/profile")
    public String showProfile(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("user", userService.getById(currentUser.getId()));
        return "auth/profile";
    }

    @PostMapping("/auth/profile")
    public String updateProfile(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                @RequestParam String fullName,
                                @RequestParam String email,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.updateProfile(currentUser.getId(), fullName, email);
            session.setAttribute(SessionKeys.CURRENT_USER, updated);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auth/profile";
    }

    @PostMapping("/auth/change-password")
    public String changePassword(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(currentUser.getId(), oldPassword, newPassword, confirmPassword);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/profile";
        }
        // Buộc đăng nhập lại với mật khẩu mới
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        return "redirect:/login";
    }

    private String dashboardOf(User user) {
        return user.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/user/dashboard";
    }
}

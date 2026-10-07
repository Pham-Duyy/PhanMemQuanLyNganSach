package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ban quản lý (SYSTEM_ADMIN) quản lý tài khoản: bổ nhiệm/thu hồi thủ quỹ, khóa/mở khóa.
 * Chỉ SYSTEM_ADMIN vào được (RoleInterceptor), các quy tắc an toàn nằm ở UserService.
 */
@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private static final String LIST_PAGE = "redirect:/admin/users";

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users/index";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                             @PathVariable Long id, @RequestParam User.Role role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.changeRole(id, role, currentUser);
            redirectAttributes.addFlashAttribute("success", role == User.Role.ADMIN
                    ? "Đã bổ nhiệm thủ quỹ!" : "Đã thu hồi quyền thủ quỹ!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/lock")
    public String lock(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                       @PathVariable Long id, RedirectAttributes redirectAttributes) {
        return setActive(id, false, currentUser, redirectAttributes, "Đã khóa tài khoản! Người này sẽ bị đăng xuất ở lần thao tác tiếp theo.");
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long id, RedirectAttributes redirectAttributes) {
        return setActive(id, true, currentUser, redirectAttributes, "Đã mở khóa tài khoản!");
    }

    private String setActive(Long id, boolean active, User actor, RedirectAttributes redirectAttributes, String successMessage) {
        try {
            userService.setActive(id, active, actor);
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }
}

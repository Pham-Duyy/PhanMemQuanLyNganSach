package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/**
 * Dashboard của user. Quyền USER được RoleInterceptor kiểm tra.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final GroupService groupService;
    private final TransactionService transactionService;

    public UserController(GroupService groupService, TransactionService transactionService) {
        this.groupService = groupService;
        this.transactionService = transactionService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        List<TransactionParticipant> paidContributions = transactionService.findPaidContributionsOfUser(currentUser.getId());

        model.addAttribute("user", currentUser);
        model.addAttribute("joinedGroups", groupService.findGroupsOfMember(currentUser.getId()).size());
        model.addAttribute("totalContributions", paidContributions.size());
        model.addAttribute("totalPaidAmount", TransactionParticipant.totalAmount(paidContributions));
        return "user/dashboard";
    }

    // Trang thông tin cá nhân dùng chung tại /auth/profile
    @GetMapping("/profile")
    public String profile() {
        return "redirect:/auth/profile";
    }

    @GetMapping("/settings")
    public String settings() {
        return "redirect:/auth/profile#doi-mat-khau";
    }
}

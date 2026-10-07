package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.Service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * Dashboard của admin. Quyền ADMIN được RoleInterceptor kiểm tra.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final GroupService groupService;
    private final TransactionService transactionService;

    public AdminController(UserService userService, GroupService groupService, TransactionService transactionService) {
        this.userService = userService;
        this.groupService = groupService;
        this.transactionService = transactionService;
    }

    @GetMapping("")
    public String home() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("totalUsers", userService.countNormalUsers());
        model.addAttribute("activeGroups", groupService.count());
        model.addAttribute("totalIncomeTransactions", transactionService.countByType(Transaction.TYPE_INCOME));
        model.addAttribute("totalExpenseTransactions", transactionService.countByType(Transaction.TYPE_EXPENSE));
        return "admin/dashboard";
    }

    // Đường dẫn cũ, trang thật nằm ở /admin/finance/transactions
    @GetMapping("/transactions")
    public String transactions() {
        return "redirect:/admin/finance/transactions";
    }
}

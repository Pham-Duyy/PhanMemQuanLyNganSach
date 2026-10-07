package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
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

    private final GroupService groupService;
    private final TransactionService transactionService;

    public AdminController(GroupService groupService, TransactionService transactionService) {
        this.groupService = groupService;
        this.transactionService = transactionService;
    }

    @GetMapping("")
    public String home() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        // Số liệu trong phạm vi quản lý: thủ quỹ thấy nhóm của mình, ban quản lý thấy mọi nhóm
        model.addAttribute("user", currentUser);
        model.addAttribute("totalUsers", groupService.countMembersOfManaged(currentUser));
        model.addAttribute("activeGroups", groupService.countManaged(currentUser));
        model.addAttribute("totalIncomeTransactions", transactionService.countManagedByType(currentUser, Transaction.TYPE_INCOME));
        model.addAttribute("totalExpenseTransactions", transactionService.countManagedByType(currentUser, Transaction.TYPE_EXPENSE));
        return "admin/dashboard";
    }

    // Đường dẫn cũ, trang thật nằm ở /admin/finance/transactions
    @GetMapping("/transactions")
    public String transactions() {
        return "redirect:/admin/finance/transactions";
    }
}
